package allanlewis.api

import kotlin.jvm.Throws

interface RestApi {

    @Throws(ApiException::class)
    fun getProduct(id: String) : Product?

    @Throws(ApiException::class)
    fun getOrder(id: String) : Order

    @Throws(ApiException::class)
    fun postOrder(order: Order) : Order

}

class ApiException: Exception {

    constructor(message: String): super(message) { }
    constructor(message: String, cause: Throwable): super(message, cause) { }

}