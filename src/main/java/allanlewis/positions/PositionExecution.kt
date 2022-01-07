package allanlewis.positions

import allanlewis.api.Order
import allanlewis.api.RestApi
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.lang.IllegalStateException
import java.time.Duration

abstract class AbstractPositionExecution(private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)

    open fun execute(order: Order): Mono<Order> {
        return restApi.postOrder(order)
            .flatMap{ o -> checkDone(o.id!!).retryWhen(Retry.backoff(10, Duration.ofMillis(100))) }
    }

    private fun checkDone(orderId: String): Mono<Order> {
        logger.info("Checking if done {}", orderId)

        return restApi.getOrder(orderId).flatMap { o ->
            logger.info("Result of check {}", o)

            if (done(o)) Mono.just(o) else Mono.error(IllegalStateException("Order not done yet"))
        }.switchIfEmpty(Mono.error(IllegalStateException("Order not found")))
    }

    abstract fun done(order: Order): Boolean

}

class OrderDone(restApi: RestApi) : AbstractPositionExecution(restApi) {

    override fun done(order: Order): Boolean {
        return "done" == order.status
    }

}

class OrderNotPending(restApi: RestApi) : AbstractPositionExecution(restApi) {

    override fun done(order: Order): Boolean {
        return "pending" != order.status
    }

}