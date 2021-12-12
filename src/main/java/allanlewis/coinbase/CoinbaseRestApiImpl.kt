package allanlewis.coinbase

import allanlewis.api.ApiException
import allanlewis.api.Order
import allanlewis.api.Product
import allanlewis.api.RestApi
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.*
import java.time.Duration.ofSeconds

class CoinbaseRestApiImpl(private val config: CoinbaseConfigurationData) : RestApi {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val timeout = 10L
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(ofSeconds(timeout)).build()

    override fun getProduct(id: String): Product? {
        return try {
            val path = "/products/$id"
            getProduct(unauthenticatedGetRequest(path))
        } catch (ex: ApiException) {
            logger.error("getProduct", ex)
            throw ex
        } catch (ex: Exception) {
            val errMsg = "getProduct"
            logger.error(errMsg, ex)
            throw ApiException(errMsg, ex)
        }
    }

    private fun getProduct(request: HttpRequest): Product? {
        logger.info("getProduct {}", request.uri())

        val response = client.send(request, BodyHandlers.ofString())

        logger.info("getProduct {} {} {}", request.uri(), response.statusCode(), response.body())

        return if (response.statusCode() == 200) {
            ObjectMapper().readValue(response.body(), Product::class.java)
        } else if (response.statusCode() == 404) {
            null
        } else {
            throw ApiException(response.statusCode().toString() + " " + response.body())
        }
    }

    override fun getOrder(id: String): Order {
        TODO("Not yet implemented")
    }

    override fun postOrder(order: Order): Order {
        TODO("Not yet implemented")
    }

    private fun unauthenticatedGetRequest(path: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create(config.restUrl + path))
            .GET()
            .header("Accept", "application/json")
            .timeout(ofSeconds(timeout)).build()
    }
}