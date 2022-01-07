package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.OrderFactory
import allanlewis.api.Product
import allanlewis.api.ReadOrder
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

    private var state = PositionState.New
    private var buyOrder: ReadOrder? = null
    private var sellOrder: ReadOrder? = null

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

    private fun buy(): Mono<ReadOrder> {
        changeState(PositionState.BuyOrderPending)

        return buy.execute(orderFactory.marketOrder(product.id, "buy", positionConfig.funds, "B" + this.id))
    }

    private fun sellPosition() {
        sell(buyOrder!!.filledSize).log().subscribe({ order ->
            changeState(order,
                PositionState.SellOrderOpen,
                PositionState.SellOrderFilled,
                PositionState.SellOrderCanceled,
                PositionState.SellOrderFailed
            )

            sellOrder = order
        }) { changeState(PositionState.SellOrderFailed) }
    }

    private fun sell(boughtSize: String): Mono<ReadOrder> {
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

        return sell.execute(orderFactory.limitOrder(product.id, "sell", price.toPlainString(), size.toPlainString(), "S" + this.id))
    }

    private fun changeState(order: ReadOrder,
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

    fun json(): Mono<PositionSummary> {
        val changes = stateChanges.map { it.copy() }.toTypedArray()

        return Mono.just(PositionSummary(this.id, this.state, buyOrder, sellOrder, changes))
    }

}

data class PositionSummary(val id: String,
                           val state: PositionState,
                           val buy: ReadOrder?,
                           val sell: ReadOrder?,
                           val changes: Array<PositionStateChange>)

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

interface PositionFactory {

    fun position(product: Product): Position

}

interface PositionStrategyFactory {

    fun strategy(name: String): PositionStrategy

}