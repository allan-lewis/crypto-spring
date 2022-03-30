package allanlewis.coinbase

import allanlewis.api.PriceTick
import allanlewis.api.WebSocketApi
import allanlewis.api.WebSocketApiImpl
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class CoinbaseWebSocketApiImpl(private val configuration: CoinbaseWebSocketConfiguration): WebSocketApi, WebSocketApiImpl {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()
    private val ticks = ConcurrentHashMap<String, PriceTick>()
    private val flux =  Flux.interval(Duration.ofMillis(500))
        .onBackpressureLatest()
        .map { ticks.values }
        .flatMapIterable { ticks -> ticks }
        .share()

    override val url = configuration.webSocketUrl

    init {
        flux.sample(Duration.ofSeconds(10)).subscribe { tick -> logger.info("Tick: {} {} {}", tick.productId, tick.price, tick.time) }
    }

    override fun ticks(productId: String): Flux<PriceTick> {
        return flux.filter { t -> t.productId == productId }
    }

    override fun send(productIds: Flux<String>): Flux<String> {
        return subscriptionPayload(productIds)
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

    private fun subscriptionPayload(productIds: Flux<String>): Flux<String> {
        val timestamp = CoinbaseUtilities.timestamp()
        val signature = CoinbaseUtilities.sign(configuration.secret, "/users/self/verify", "GET", "", timestamp)

        return productIds.collectList().flatMapMany { ids  -> Mono.just(SubscriptionMessage(signature,
            configuration.key,
            configuration.passphrase,
            timestamp,
            ids.toTypedArray())) }.map { m -> jacksonObjectMapper().writeValueAsString(m) }
    }

}

interface CoinbaseWebSocketConfiguration : CoinbaseRestApiImpl.CoinbaseRestApiConfiguration {

    val webSocketUrl: String

}
