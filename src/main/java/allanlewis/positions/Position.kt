package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.Order
import allanlewis.api.OrderFactory
import allanlewis.api.Product
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Position(private val positionConfig: PositionConfig,
               private val product: Product,
               private val buy: AbstractPositionExecution,
               private val sell: AbstractPositionExecution,
               private val orderFactory: OrderFactory) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val stateChanges = ArrayList<PositionStateChange>()
    private val mapper = ObjectMapper()

    private var state = PositionState.New
    private var buyOrder: Order? = null
    private var sellOrder: Order? = null

    val id = UUID.randomUUID().toString()

    fun init(): Position {
        changeState(PositionState.Started)
        buyPosition()
        return this
    }

    private fun buyPosition() {
        buy().log().subscribe({ order ->
            changeState(order,
                null,
                PositionState.BuyOrderFilled,
                PositionState.BuyOrderCanceled,
                PositionState.BuyOrderFailed
            )
            buyOrder = order
        }, { changeState(PositionState.BuyOrderFailed) }) {
            if (state === PositionState.BuyOrderFilled) {
                sellPosition()
            }
        }
    }

    private fun buy(): Mono<Order> {
        changeState(PositionState.BuyOrderPending)

        val order = orderFactory.order()
        order.productId = product.id
        order.side = "buy"
        order.type = "market"
        order.funds = positionConfig.funds
        order.clientId = "B" + this.id

        return buy.execute(order)
    }

    private fun sellPosition() {
        sell(buyOrder!!.filledSize!!).log().subscribe({ order ->
            changeState(order,
                PositionState.SellOrderOpen,
                PositionState.SellOrderFilled,
                PositionState.SellOrderCanceled,
                PositionState.SellOrderFailed
            )

            sellOrder = order
        }) { changeState(PositionState.SellOrderFailed) }
    }

    private fun sell(boughtSize: String): Mono<Order> {
        changeState(PositionState.SellOrderPending)

        val fee = BigDecimal.ONE.add(BigDecimal(positionConfig.fee))

        val funds: BigDecimal = BigDecimal(positionConfig.funds)
            .multiply(fee)
            .setScale(BigDecimal(product.quoteIncrement).scale(), RoundingMode.CEILING)

        val size: BigDecimal = BigDecimal(boughtSize)
            .multiply(BigDecimal(positionConfig.sell))
            .setScale(BigDecimal(product.baseIncrement).scale(), RoundingMode.FLOOR)
        val price = funds.divide(size, RoundingMode.HALF_UP).setScale(0, RoundingMode.CEILING)

        logger.info("Selling {} {} {} {} {}", boughtSize, fee, funds, size, price)

        val order = orderFactory.order()
        order.productId = product.id
        order.side = "sell"
        order.size = size.toPlainString()
        order.price = price.toPlainString()
        order.clientId = "S" + this.id

        return sell.execute(order)
    }

    private fun changeState(order: Order,
                            openState: PositionState?,
                            filledState: PositionState,
                            cancelledState: PositionState,
                            defaultState: PositionState) {

        if (openState != null && "open" == order.status) {
            changeState(openState)
        } else if ("done" == order.status && "filled" == order.doneReason) {
            changeState(filledState)
        } else if ("done" == order.status && "canceled" == order.doneReason) {
            changeState(cancelledState)
        } else {
            changeState(defaultState)
        }
    }

    private fun changeState(newState: PositionState) {
        val oldState = this.state
        this.state = newState

        stateChanges.add(PositionStateChange(oldState,
            newState,
            ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME)))
    }

    fun json(): Mono<PositionJson> {
        val buy = mapper.readValue(mapper.writeValueAsString(buyOrder), Order::class.java)
        val sell = mapper.readValue(mapper.writeValueAsString(sellOrder), Order::class.java)
        val changes = stateChanges.map { it.copy() }.toTypedArray()

        return Mono.just(PositionJson(this.id, this.state, buy, sell, changes))
    }

}

data class PositionStateChange(val oldState: PositionState, val newState: PositionState, val timestamp: String)

enum class PositionState {

    New,
    Started,
    BuyOrderPending,
    BuyOrderFilled,
    BuyOrderFailed,
    BuyOrderCanceled,
    SellOrderPending,
    SellOrderOpen,
    SellOrderFilled,
    SellOrderCanceled,
    SellOrderFailed

}

data class PositionJson(val id: String,
                        val state: PositionState,
                        val buy: Order?,
                        val sell: Order?,
                        val changes: Array<PositionStateChange>)

interface PositionFactory {

    fun position(product: Product): Position

}

interface PositionStrategyFactory {

    fun strategy(name: String): PositionStrategy

}