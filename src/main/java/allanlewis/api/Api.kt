package allanlewis.api

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.jvm.Throws

interface WebSocketApi {

    @Throws(ApiException::class)
    fun ticks(): Flux<PriceTick>

}

interface RestApi {

    @Throws(ApiException::class)
    fun getProduct(id: String) : Mono<Product>

    @Throws(ApiException::class)
    fun getOrder(id: String) : Mono<Order>

    @Throws(ApiException::class)
    fun getOrders(): Collection<Order>

    @Throws(ApiException::class)
    fun postOrder(order: Order) : Order
}

class ApiException: Exception {

    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

interface Product {

    var id: String?
    var baseCurrency: String?
    var quoteCurrency: String?
    var baseMinSize: String?
    var baseMaxSize: String?
    var quoteIncrement: String?
    var baseIncrement: String?
    var displayName: String?
    var minMarketFunds: String?
    var maxMarketFunds: String?
    var maxSlippagePercentage: String?
    var marginEnabled: Boolean
    var postOnly: Boolean
    var limitOnly: Boolean
    var cancelOnly: Boolean
    var fxStableCoin: Boolean
    var tradingDisabled: Boolean
    var status: String?
    var statusMessage: String?
    var auctionMode: Boolean
}

interface Order {

    var id: String?
    var clientId: String?
    var price: String?
    var size: String?
    var productId: String?
    var profileId: String?
    var side: String?
    var type: String?
    var timeInForce: String?
    var postOnly: Boolean
    var createdAt: String?
    var fillFees: String?
    var filledSize: String?
    var executedValue: String?
    var status: String?
    var settled: Boolean
    var stop: String?
    var funds: String?
    var specifiedFunds: String?
    var doneAt: String?
    var doneReason: String?
}

interface PriceTick {

    var price: String
    var productId: String
    var time: String
    var twentyFourHourOpen: String
    var twentyFourHourVolume: String
    var twentyFourHourHigh: String
    var twentyFourHourLow: String

}
