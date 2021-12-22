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
        return apiCall(unauthenticatedRequest("/products/$id", "GET"),
            "getProduct",
            CoinbaseProduct::class.java, arrayOf(404))
    }

    override fun getOrder(id: String): Order? {
        return apiCall(authenticatedRequest("/orders/$id", "GET", "", CoinbaseUtilities.timestamp()),
            "getOrder",
            CoinbaseOrder::class.java,
            arrayOf(404))
    }

    override fun postOrder(order: Order): Order {
        val body = ObjectMapper().writeValueAsString(order)
        return apiCall(authenticatedRequest("/orders", "POST", body, CoinbaseUtilities.timestamp()),
            "postOrder",
            CoinbaseOrder::class.java)!!
    }

    private fun <T> apiCall(request: HttpRequest, logPrefix: String, valueType: Class<T>): T? {
        return apiCall(request, logPrefix, valueType, emptyArray())
    }

    private fun <T> apiCall(request: HttpRequest, logPrefix: String, valueType: Class<T>, nullStatuses: Array<Int>): T? {
        return try {
            send(request, logPrefix, valueType, nullStatuses)
        } catch (ex: ApiException) {
            logger.error(logPrefix, ex)
            throw ex
        } catch (ex: Exception) {
            logger.error(logPrefix, ex)
            throw ApiException(logPrefix, ex)
        }
    }

    private fun <T> send(request: HttpRequest, logPrefix: String, valueType: Class<T>, nullStatuses: Array<Int>): T? {
        logger.info("{} {}", logPrefix, request.uri())

        val response = client.send(request, BodyHandlers.ofString())

        logger.info("{} {} {} {}", logPrefix, request.uri(), response.statusCode(), response.body())

        return if (response.statusCode() == 200) {
            ObjectMapper().readValue(response.body(), valueType)
        } else if (nullStatuses.indexOf(response.statusCode()) != -1) {
            null
        } else {
            throw ApiException(logPrefix + " " + response.statusCode())
        }
    }

    private fun unauthenticatedRequest(path: String, method: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create(config.restUrl + path))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .header("Accept", "application/json")
            .timeout(ofSeconds(timeout)).build()
    }

    private fun authenticatedRequest(path: String, method: String, body: String, timestamp: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create(config.restUrl + path))
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("cb-access-key", config.key)
            .header("cb-access-passphrase", config.passphrase)
            .header("cb-access-sign", CoinbaseUtilities.sign(config.secret, path, method, body, timestamp))
            .header("cb-access-timestamp", timestamp)
            .timeout(ofSeconds(timeout)).build()
    }
}
