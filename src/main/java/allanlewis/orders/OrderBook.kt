package allanlewis.orders

import allanlewis.api.Order
import allanlewis.api.RestApi
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class OrderBook(private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val orders = AtomicReference<Collection<Order>>()

    fun init(): OrderBook {
        orders.set(restApi.getOrders())

        Flux.interval(Duration.ofMinutes(1)).map { orders.set(restApi.getOrders()) }.subscribe {
            logger.info("{}", orders.get())
        }

        return this
    }

    fun orders(): Collection<Order> {
        return Collections.unmodifiableCollection(orders.get())
    }

}