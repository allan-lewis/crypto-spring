package allanlewis.positions

import allanlewis.api.Order
import allanlewis.api.RestApi
import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

abstract class AbstractPositionExecution(private val doneCheck: Function<Order, Boolean>, private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val order: AtomicReference<Order> = AtomicReference<Order>()

    @JsonIgnore
    lateinit var mono: Mono<Order>

    open fun init(order: Order): AbstractPositionExecution? {
        this.order.set(order)
        mono = Mono.create { sink: MonoSink<Order> ->
            try {
                this.order.set(restApi.postOrder(this.order.get()))
                while (orderNotDone()) {
                    Thread.sleep(500)
                }
                sink.success(this.order.get())
            } catch (ex: Exception) {
                sink.error(ex)
            }
        }

        return this
    }

    private fun orderNotDone(): Boolean {
        return try {
            val order = restApi.getOrder(order.get().id!!)

            if (order != null) {
                this.order.set(order)
                !doneCheck.apply(this.order.get())
            } else {
                logger.warn("Order {} not found yet", this.order.get().id)
                true
            }
        } catch (ex: Exception) {
            throw RuntimeException("Unable to check order status", ex)
        }
    }

    open fun order(): Order {
        return order.get()
    }

}

class OrderDone(restApi: RestApi) : AbstractPositionExecution(Function { order -> "done" == order.status }, restApi)

class OrderNotPending(restApi: RestApi) : AbstractPositionExecution(Function { order -> "pending" != order.status }, restApi)