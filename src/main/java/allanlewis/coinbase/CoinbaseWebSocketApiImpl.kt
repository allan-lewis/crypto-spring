package allanlewis.coinbase

import allanlewis.api.PriceTick
import allanlewis.api.WebSocketApi
import allanlewis.coinbase.CoinbaseUtilities.sign
import allanlewis.coinbase.CoinbaseUtilities.timestamp
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.StandardWebSocketClient
import org.springframework.web.socket.WebSocketHttpHeaders
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.concurrent.CompletableFuture
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

    override fun ticks(productIds: Array<String>): Flux<PriceTick> {
        return webSocketHandler.subscribe(productIds)
    }

}

class CoinbaseWebSocketHandler(private val config: CoinbaseConfigurationData) : WebSocketHandler {

    private val mapper = jacksonObjectMapper()
    private val ticks = ConcurrentHashMap<String, PriceTick>()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val flux =  Flux.interval(Duration.ofMillis(500))
        .onBackpressureDrop()
        .map { ticks.values }
        .flatMapIterable { ticks -> ticks }
        .share()

    private val future = CompletableFuture<WebSocketSession>()

    fun subscribe(productIds: Array<String>): Flux<PriceTick> {
        return Mono.fromFuture(future)
            .flatMapMany { s ->
                logger.info("Subscribing for {}", productIds)

                s.send(Mono.just(s.textMessage(subscriptionPayload(productIds))))
                .thenMany(flux) }
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        future.complete(session)

        return session.receive().map { webSocketMessage -> handleResponse(webSocketMessage) }.then()
    }

    private fun subscriptionPayload(ids: Array<String>): String {
        val timestamp = timestamp()
        val signature = sign(config.secret, "/users/self/verify", "GET", "", timestamp)

        val message = SubscriptionMessage (signature,
            config.key,
            config.passphrase,
            timestamp,
            ids
        )

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

}
