package allanlewis.positions

import allanlewis.api.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.lang.IllegalStateException
import java.time.Duration

abstract class AbstractPositionExecution(private val restApi: RestApi, private val retries: Long) {

    private val logger = LoggerFactory.getLogger(javaClass)

    open fun execute(order: WriteOrder): Mono<ReadOrder> {
        return restApi.postOrder(order)
            .flatMap { o -> checkDone(o.id) }
            .retryWhen(Retry.backoff(retries, Duration.ofMillis(100)))
    }

    private fun checkDone(orderId: String): Mono<ReadOrder> {
        logger.info("Checking if done {}", orderId)

        return restApi.getOrder(orderId).flatMap { order ->
            val done = done(order)
            logger.info("Result of check {} {}",done, order)

            if (done) Mono.just(order) else Mono.empty()
        }.switchIfEmpty(Mono.error(IllegalStateException("Order not found")))
    }

    abstract fun done(order: ReadOrder): Boolean

}

class OrderDone(restApi: RestApi, retries: Long) : AbstractPositionExecution(restApi, retries) {

    override fun done(order: ReadOrder): Boolean {
        return "done" == order.status
    }

}

class OrderNotPending(restApi: RestApi, retries: Long) : AbstractPositionExecution(restApi, retries) {

    override fun done(order: ReadOrder): Boolean {
        return "pending" != order.status
    }

}