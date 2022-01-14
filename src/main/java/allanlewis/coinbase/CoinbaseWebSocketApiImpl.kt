package allanlewis.coinbase

import allanlewis.api.PriceTick
import allanlewis.api.WebSocketApi
import allanlewis.api.WebSocketBridge
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class CoinbaseWebSocketApiImpl(private val configuration: CoinbaseWebSocketConfiguration): WebSocketApi, WebSocketBridge {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()
    private val ticks = ConcurrentHashMap<String, PriceTick>()
    private val flux =  Flux.interval(Duration.ofMillis(500))
        .onBackpressureDrop()
        .map { ticks.values }
        .flatMapIterable { ticks -> ticks }
        .share()
    private val future = CompletableFuture<Array<String>>()

    override fun ticks(productIds: Array<String>): Flux<PriceTick> {
        if (!future.isDone) {
            logger.info("Getting ticks for {}", productIds)

            future.complete(productIds)
        } else {
            logger.warn("Ignored repeated ticks request")
        }

        return flux
    }

    override fun send(): Flux<String> {
        return Mono.fromFuture(future).flatMapMany { array -> subscriptionPayload(array)}
    }

    override fun receive(message: String) {
        val coinbaseWebSocketMessage = mapper.readValue(message, CoinbaseWebSocketMessage::class.java)

        logger.debug("{}", message)

        when (coinbaseWebSocketMessage.type) {
            "ticker" -> ticks[coinbaseWebSocketMessage.productId] = CoinbasePriceTick(coinbaseWebSocketMessage.price,
                coinbaseWebSocketMessage.productId,
                coinbaseWebSocketMessage.time,
                coinbaseWebSocketMessage.twentyFourHourOpen,
                coinbaseWebSocketMessage.twentyFourHourVolume,
                coinbaseWebSocketMessage.twentyFourHourHigh,
                coinbaseWebSocketMessage.twentyFourHourLow)
        }
    }

    private fun subscriptionPayload(ids: Array<String>): Flux<String> {
        val timestamp = CoinbaseUtilities.timestamp()
        val signature = CoinbaseUtilities.sign(configuration.secret, "/users/self/verify", "GET", "", timestamp)

        val message = SubscriptionMessage (signature,
            configuration.key,
            configuration.passphrase,
            timestamp,
            ids
        )

        return Flux.just(jacksonObjectMapper().writeValueAsString(message))
    }

}

interface CoinbaseWebSocketConfiguration : CoinbaseRestApiImpl.CoinbaseRestApiConfiguration {

    val webSocketUrl: String

}
