package allanlewis.positions

import allanlewis.api.PriceTick
import allanlewis.api.WebSocketApi
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.tuple.Tuple2
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap

interface PositionStrategy {

    fun openPosition(productId: String): Mono<Boolean>

}

abstract class AbstractPositionStrategy: PositionStrategy {

    val logger = LoggerFactory.getLogger(javaClass)!!

    override fun openPosition(productId: String): Mono<Boolean> {
        return Mono.just(open(productId))
    }

    abstract fun open(productId: String): Boolean

}

class AlwaysTrueStrategy : AbstractPositionStrategy() {

    override fun open(productId: String): Boolean {
        logger.info("Always opening a position for $productId")

        return true
    }

}

class AlwaysFalseStrategy : AbstractPositionStrategy() {

    override fun open(productId: String): Boolean {
        logger.info("Never opening a position for $productId")

        return false
    }

}

class DayRangeStrategy(private val webSocketApi: WebSocketApi) : AbstractPositionStrategy() {

    private val decisions = ConcurrentHashMap<String, Tuple2<Boolean, String>>()

    fun init(): DayRangeStrategy {
        webSocketApi.ticks().subscribe { tick -> decide(tick) }

        return this
    }

    private fun decide(tick: PriceTick) {
        logger.debug("{}", tick)

        val high = BigDecimal(tick.twentyFourHourHigh)
        val low = BigDecimal(tick.twentyFourHourLow)

        val increment = high.subtract(low).divide(BigDecimal(4), RoundingMode.HALF_UP)
        val start = low.add(increment)
        val end = high.subtract(increment.multiply(BigDecimal(2)))

        val price = BigDecimal(tick.price)
        val decision = price < end && price > start

        logger.debug("{} {} {}...{}...{}...{} {}", tick.productId, tick.price, low.toPlainString(), start, end, high.toPlainString(), decision)

        decisions[tick.productId] = Tuple2.of(decision, "" + price + "[" + start.toPlainString() + ", " + end.toPlainString() + "] " + "[" + low.toPlainString() + ", " + high.toPlainString() + "]")
    }

    override fun open(productId: String): Boolean {
        val decision = decisions.getOrDefault(productId, Tuple2.of(false, "undefined"))

        // Log here rather than from message() to avoid a race condition
        logger.info("Open position for $productId: ${decision.t1} ${decision.t2}")

        return false
//        return decision
    }

}
