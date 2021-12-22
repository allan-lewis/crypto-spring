package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.Order
import allanlewis.api.Product
import allanlewis.coinbase.CoinbaseOrder
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Position(private val positionConfig: PositionConfig,
               private val product: Product,
               private val applicationContext: ApplicationContext) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val buy = applicationContext.getBean(OrderDone::class.java)
    private val sell = applicationContext.getBean(OrderNotPending::class.java)
    private val stateChanges = ArrayList<PositionStateChange>()

    private var state: PositionState = PositionState.New

    val id = UUID.randomUUID().toString()

    @JsonProperty("buyOrder")
    var buyOrder: Order?  = null
        private set
    @JsonProperty("sellOrder")
    var sellOrder: Order? = null
        private set

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

        val order = applicationContext.getBean(Order::class.java)
        order.productId = product.id
        order.side = "buy"
        order.type = "market"
        order.funds = positionConfig.funds

        return buy.init(order)!!.mono
    }

    private fun sellPosition() {
        sell(buy.order().filledSize).log().subscribe({ order ->
            changeState(order,
                PositionState.SellOrderOpen,
                PositionState.SellOrderFilled,
                PositionState.SellOrderCanceled,
                PositionState.SellOrderFailed
            )

            sellOrder = order
        }) { changeState(PositionState.SellOrderFailed) }
    }

    private fun sell(boughtSize: String?): Mono<Order> {
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

        val order = applicationContext.getBean(Order::class.java)
        order.productId = product.id
        order.side = "sell"
        order.size = size.toPlainString()
        order.price = price.toPlainString()

        return sell.init(order)!!.mono
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
        } else if ("done" == order.status && "canceled".equals(order.doneReason)) {
            changeState(cancelledState)
        } else {
            changeState(defaultState)
        }
    }

    private fun changeState(newState: PositionState) {
        val oldState = this.state
        this.state = newState
        stateChanges.add(PositionStateChange(oldState, newState))
    }

    @JsonProperty("stateChanges")
    fun stateChanges(): List<PositionStateChange> {
        return Collections.unmodifiableList(stateChanges)
    }

}

class PositionStateChange(val oldState: PositionState, val newState: PositionState) {

    val timestamp: String = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME)

}

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