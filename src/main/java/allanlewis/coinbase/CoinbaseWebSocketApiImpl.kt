package allanlewis.coinbase

import allanlewis.api.PriceTick
import allanlewis.api.WebSocketApi
import allanlewis.coinbase.CoinbaseUtilities.sign
import allanlewis.coinbase.CoinbaseUtilities.timestamp
import allanlewis.products.ProductRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.StandardWebSocketClient
import org.springframework.web.socket.WebSocketHttpHeaders
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.websocket.ContainerProvider

class CoinbaseWebSocketApiImpl(private val config: CoinbaseConfigurationData,
                               private val webSocketHandler: CoinbaseWebSocketHandler): WebSocketApi {

    private val size = 2048000

    fun init(): CoinbaseWebSocketApiImpl {
        val container = ContainerProvider.getWebSocketContainer()
        container.defaultMaxTextMessageBufferSize = size
        container.defaultMaxBinaryMessageBufferSize = size

        val webSocketClient = StandardWebSocketClient(container)
        val headers = WebSocketHttpHeaders()
        val uri = URI.create(config.webSocketUrl)
        webSocketClient.execute(uri, headers, webSocketHandler).subscribe()

        return this
    }

    override fun ticks(): Flux<PriceTick> {
        return webSocketHandler.ticks()
    }

}

class CoinbaseWebSocketHandler(private val config: CoinbaseConfigurationData,
                               private val productRepository: ProductRepository) : WebSocketHandler {

    private val mapper = jacksonObjectMapper()
    private val ticks = ConcurrentHashMap<String, PriceTick>()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val flux =  Flux.interval(Duration.ofMillis(500))
        .onBackpressureDrop()
        .map { ticks.values }
        .flatMapIterable { ticks -> ticks }
        .share()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val subscription = productRepository.products().map { p ->
            logger.info("Subscribing for {}", p.id)

            p.id
        }.collectList().map { list -> subscriptionPayload(list) }

        return session.send(subscription
            .map(session::textMessage))
            .and(session.receive().map { webSocketMessage -> handleResponse(webSocketMessage) }.log())
            .and(session.closeStatus().map(CloseStatus::getCode).log())
    }

    private fun subscriptionPayload(ids: List<String>): String {
        val timestamp = timestamp()
        val signature = sign(config.secret, "/users/self/verify", "GET", "", timestamp)

        val message = SubscriptionMessage ()
        message.signature = signature
        message.key = config.key
        message.passphrase = config.passphrase
        message.timestamp = timestamp
        message.productIds = ids.toTypedArray()

        return jacksonObjectMapper().writeValueAsString(message)
    }

    private fun handleResponse(message: WebSocketMessage): CoinbaseWebSocketMessage {
        val coinbaseWebSocketMessage = mapper.readValue(message.payloadAsText, CoinbaseWebSocketMessage::class.java)

        logger.debug("{}", message.payloadAsText)

        when (coinbaseWebSocketMessage.type) {
            "ticker" -> updatePrice(coinbaseWebSocketMessage)
        }

        return coinbaseWebSocketMessage
    }

    private fun updatePrice(message: CoinbaseWebSocketMessage) {
        ticks[message.productId] = CoinbasePriceTick(message.price,
                message.productId,
                message.time,
                message.twentyFourHourOpen,
                message.twentyFourHourVolume,
                message.twentyFourHourHigh,
                message.twentyFourHourLow)
    }

    fun ticks(): Flux<PriceTick> {
        return flux
    }

}
