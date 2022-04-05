package allanlewis.api

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.jvm.Throws

interface RestApi {

    @Throws(ApiException::class)
    fun getProduct(id: String) : Mono<Product>

    @Throws(ApiException::class)
    fun getOrder(id: String) : Mono<ReadOrder>

    @Throws(ApiException::class)
    fun getOrders(): Flux<ReadOrder>

    @Throws(ApiException::class)
    fun postOrder(order: WriteOrder) : Mono<ReadOrder>

    @Throws(ApiException::class)
    fun getAccounts() : Flux<Account>

    @Throws(ApiException::class)
    fun getAccount(id: String) : Mono<Account>

}

class ApiException: Exception {

    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}

interface Account {

    val id: String
    val currency: String
    val balance: String

}

interface Product {

    val id: String
    val quoteIncrement: String
    val baseIncrement: String
    val baseCurrency: String
    val quoteCurrency: String

}

interface BaseOrder {

    val type: String
    val side: String
    val productId: String

}

interface ReadOrder: BaseOrder {

    val id: String
    val status: String
    val doneReason: String
    val filledSize: String

}

interface WriteOrder: BaseOrder {

    val profileId: String
    val clientId: String

}

interface MarketOrder: WriteOrder {

    val funds: String

}

interface LimitOrder : WriteOrder {

    val price: String
    val size: String

}

interface OrderFactory {

    fun marketOrder(productId: String, side: String, funds: String, clientId: String): MarketOrder

    fun limitOrder(productId: String, side: String, price: String, size: String, clientId: String): LimitOrder

}

interface WebSocketApi {

    @Throws(ApiException::class)
    fun ticks(productId: String): Flux<PriceTick>

}

interface PriceTick {

    val price: String
    val productId: String
    val time: String
    val twentyFourHourOpen: String
    val twentyFourHourVolume: String
    val twentyFourHourHigh: String
    val twentyFourHourLow: String

    fun stale(): Boolean

}

data class PositionConfig(val id: String,
                          val max: Int,
                          val funds: String,
                          val fee: String,
                          val sell: String,
                          val strategy: String)

interface WebSocketApiImpl {

    val url: String

    fun send(productIds: Flux<String>): Flux<String>

    fun receive(message: String)

}