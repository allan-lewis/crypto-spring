package allanlewis.coinbase

import allanlewis.PositionConfig
import allanlewis.api.PriceTick
import allanlewis.api.Product
import allanlewis.api.WebSocketApi
import allanlewis.coinbase.CoinbaseUtilities.sign
import allanlewis.coinbase.CoinbaseUtilities.timestamp
import allanlewis.products.ProductRepository
import com.fasterxml.jackson.databind.ObjectMapper
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
import kotlin.collections.ArrayList

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
                               private val positionConfigs: Array<PositionConfig>,
                               private val productRepository: ProductRepository) : WebSocketHandler {

    private val mapper = ObjectMapper()
    private val ticks = ConcurrentHashMap<String, PriceTick>()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val flux =  Flux.interval(Duration.ofMillis(500))
        .onBackpressureDrop()
        .map { ticks.values }
        .flatMapIterable { ticks -> ticks }
        .share()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val products = ArrayList<Mono<Product>>()
        for (pc in positionConfigs) {
            products.add(productRepository.product(pc.id))
        }

        val ids = Flux.merge(products).toIterable().map { p -> p.id!! }

        logger.info("Subscribing for {}", ids)

        val payload = subscriptionPayload(ids)

        return session.send(Mono.just(payload)
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

        return ObjectMapper().writeValueAsString(message)
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
