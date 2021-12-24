package allanlewis.positions

import allanlewis.api.PriceTick
import allanlewis.api.WebSocketApi
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

interface PositionStrategy {

    fun openPosition(productId: String): Mono<Boolean>

}

class AlwaysTrueStrategy() : PositionStrategy {

    override fun openPosition(productId: String): Mono<Boolean> {
        return Mono.just(true)
    }

}

class DayRangeStrategy(private val webSocketApi: WebSocketApi) : PositionStrategy {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val decisions = ConcurrentHashMap<String, Boolean>()

    fun init(): DayRangeStrategy {
        webSocketApi.ticks().subscribe { tick -> decide(tick) }

        return this
    }

    private fun decide(tick: PriceTick) {
        logger.debug("{}", tick)

        decisions[tick.productId] = false
    }

    override fun openPosition(productId: String): Mono<Boolean> {
        val open = decisions.getOrDefault(productId, false)

        logger.info("Open position for {}? {}", productId, open)

        return Mono.just(false)
    }

}
