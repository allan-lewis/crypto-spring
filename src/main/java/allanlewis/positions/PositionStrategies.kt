package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.PriceTick
import allanlewis.api.RestApi
import allanlewis.api.WebSocketApi
import allanlewis.products.ProductRepository
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
        return open(productId)
    }

    abstract fun open(productId: String): Mono<Boolean>

}

class AlwaysTrueStrategy : AbstractPositionStrategy() {

    override fun open(productId: String): Mono<Boolean> {
        logger.info("Always opening a position for $productId")

        return Mono.just(true)
    }

}

class AlwaysFalseStrategy : AbstractPositionStrategy() {

    override fun open(productId: String): Mono<Boolean> {
        logger.info("Never opening a position for $productId")

        return Mono.just(false)
    }

}

abstract class AbstractCheckFundsStrategy : AbstractPositionStrategy() {

    override fun open(productId: String): Mono<Boolean> {
        return checkFunds(productId).flatMap { funds -> if (funds) {
            logger.info("Sufficient  funds for {}", productId)

            shouldOpen(productId)
        } else {
            logger.info("Insufficient funds for {}", productId)

            Mono.just(false) }
        }
    }

    abstract fun checkFunds(productId: String): Mono<Boolean>

    abstract fun shouldOpen(productId: String): Mono<Boolean>

}

class DayRangeStrategy(private val positionConfigs: Array<PositionConfig>,
                       private val productRepository: ProductRepository,
                       private val restApi: RestApi,
                       private val webSocketApi: WebSocketApi) : AbstractCheckFundsStrategy() {

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
        val end = high.subtract(increment)

        val price = BigDecimal(tick.price)
        val decision = price < end && price > start

        logger.debug("{} {} {}...{}...{}...{} {}", tick.productId, tick.price, low.toPlainString(), start, end, high.toPlainString(), decision)

        decisions[tick.productId] = Tuple2.of(decision, "" + price + " [" + start.toPlainString() + ", " + end.toPlainString() + "] " + "[" + low.toPlainString() + ", " + high.toPlainString() + "]")
    }

    override fun shouldOpen(productId: String): Mono<Boolean> {
        val decision = decisions.getOrDefault(productId, Tuple2.of(false, "undefined"))

        logger.info("Open position for $productId: ${decision.t1} ${decision.t2}")

        return Mono.just(decision.t1)
    }

    override fun checkFunds(productId: String): Mono<Boolean> {
        var min = BigDecimal.ZERO
        for (pc in positionConfigs) {
            if (pc.id == productId) {
                min = BigDecimal(pc.funds)
            }
        }

        return productRepository.product(productId)
            .flatMap { p -> restApi.getAccounts().filter { a -> a.currency == p.quoteCurrency }.next() }
            .map { account ->
                logger.info("For currency {} have balance {} (min {})", account.currency, account.balance, min)

                BigDecimal(account.balance) > min
            }
    }

}
