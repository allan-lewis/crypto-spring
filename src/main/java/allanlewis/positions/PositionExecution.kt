package allanlewis.positions

import allanlewis.api.Order
import allanlewis.api.RestApi
import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

abstract class AbstractPositionExecution(private val doneCheck: Function<Order, Boolean>, private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val order: AtomicReference<Order> = AtomicReference<Order>()
    private val orderDone = AtomicBoolean()

    @JsonIgnore
    lateinit var mono: Mono<Order>

    open fun init(order: Order): AbstractPositionExecution? {
        this.order.set(order)
        mono = Mono.create { sink: MonoSink<Order> ->
            try {
                this.order.set(restApi.postOrder(this.order.get()))

                while (!orderDone.get()) {
                    Thread.sleep(500)
                    check()
                }
                sink.success(this.order.get())
            } catch (ex: Exception) {
                sink.error(ex)
            }
        }

        return this
    }

    private fun check() {
        restApi.getOrder(this.order.get().id!!).subscribe({ o ->
                this.order.set(o)
                orderDone.set(doneCheck.apply(this.order.get()))
            },
            {
                throwable -> logger.error("Error getting order", throwable)
            },
            {
                logger.info("Order done ? {} {}", this.order.get(), orderDone.get())
            })
    }

    open fun order(): Order {
        return order.get()
    }

}

class OrderDone(restApi: RestApi) : AbstractPositionExecution(Function { order -> "done" == order.status }, restApi)

class OrderNotPending(restApi: RestApi) : AbstractPositionExecution(Function { order -> "pending" != order.status }, restApi)