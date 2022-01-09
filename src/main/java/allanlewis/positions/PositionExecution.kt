package allanlewis.positions

import allanlewis.api.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.lang.IllegalStateException
import java.time.Duration

abstract class AbstractPositionExecution(private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)

    open fun execute(order: WriteOrder): Mono<ReadOrder> {
        return restApi.postOrder(order)
            .flatMap{ o -> checkDone(o.id).retryWhen(Retry.backoff(5, Duration.ofMillis(100))) }
    }

    private fun checkDone(orderId: String): Mono<ReadOrder> {
        logger.info("Checking if done {}", orderId)

        return restApi.getOrder(orderId)
            .switchIfEmpty(Mono.error(IllegalStateException("Order not found")))
            .flatMap { o ->
                val done = done(o)

                logger.info("Result of check {} {}",done, o)

                if (done) Mono.just(o) else Mono.error(IllegalStateException("Order not done yet"))
            }
    }

    abstract fun done(order: ReadOrder): Boolean

}

class OrderDone(restApi: RestApi) : AbstractPositionExecution(restApi) {

    override fun done(order: ReadOrder): Boolean {
        return "done" == order.status
    }

}

class OrderNotPending(restApi: RestApi) : AbstractPositionExecution(restApi) {

    override fun done(order: ReadOrder): Boolean {
        return "pending" != order.status
    }

}