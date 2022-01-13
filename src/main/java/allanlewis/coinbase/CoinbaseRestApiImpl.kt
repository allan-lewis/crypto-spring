package allanlewis.coinbase

import allanlewis.api.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.*
import java.time.Duration.ofSeconds

class CoinbaseRestApiImpl(private val config: CoinbaseRestApiConfiguration) : RestApi {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val timeout = 10L
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(ofSeconds(timeout)).build()
    private val mapper = jacksonObjectMapper()

    override fun getProduct(id: String): Mono<Product> {
        return response(unauthenticatedGetRequest("/products/$id"),
            "getProduct",
                arrayOf(404)).map { s -> mapper.readValue<CoinbaseProduct>(s) }
    }

    override fun getOrder(id: String): Mono<ReadOrder> {
        return response(authenticatedRequest("/orders/$id", "GET", "", CoinbaseUtilities.timestamp()),
            "getOrder",
                arrayOf(404)).map { s -> mapper.readValue<CoinbaseOrder>(s) }
    }

    override fun getOrders(): Flux<ReadOrder> {
        return response(authenticatedRequest("/orders", "GET", "", CoinbaseUtilities.timestamp()),
                "getOrders", emptyArray())
            .map { s -> mapper.readValue<List<CoinbaseOrder>>(s) }
            .flatMapIterable { l -> l }
    }

    override fun postOrder(order: WriteOrder): Mono<ReadOrder> {
        val body = jacksonObjectMapper().writeValueAsString(order)

        return response(authenticatedRequest("/orders", "POST", body, CoinbaseUtilities.timestamp()),
            "postOrder",
            emptyArray()).map { s -> mapper.readValue<CoinbaseOrder>(s) }
    }

    override fun getAccount(id: String): Mono<Account> {
        return response(authenticatedRequest("/accounts/$id", "GET", "", CoinbaseUtilities.timestamp()),
            "getAccount",
            arrayOf(404))
            .map { s -> mapper.readValue<CoinbaseAccount>(s)}
    }

    override fun getAccounts(): Flux<Account> {
        return response(authenticatedRequest("/accounts", "GET", "", CoinbaseUtilities.timestamp()),
            "getAccounts",
            emptyArray(), false)
            .map { s -> mapper.readValue<List<CoinbaseAccount>>(s) }
            .flatMapIterable { l -> l }
    }

    private fun response(request: HttpRequest, logPrefix: String, nullStatuses: Array<Int>): Mono<String> {
        return response(request, logPrefix, nullStatuses, true)
    }

    private fun response(request: HttpRequest, logPrefix: String, nullStatuses: Array<Int>, log: Boolean): Mono<String> {
        return try {
            val response = apiCall(request, logPrefix, nullStatuses, log)

            return if (response != null) {
                Mono.just(response)
            } else {
                Mono.empty()
            }
        } catch (ex: ApiException) {
            Mono.error(ex)
        }
    }

    private fun apiCall(request: HttpRequest, logPrefix: String, nullStatuses: Array<Int>, log: Boolean): String? {
        return try {
            send(request, logPrefix, nullStatuses, log)
        } catch (ex: ApiException) {
            logger.error(logPrefix, ex)
            throw ex
        } catch (ex: Exception) {
            logger.error(logPrefix, ex)
            throw ApiException(logPrefix, ex)
        }
    }

    private fun send(request: HttpRequest, logPrefix: String, nullStatuses: Array<Int>, log: Boolean): String? {
        logger.info("{} {}", logPrefix, request.uri())

        val response = client.send(request, BodyHandlers.ofString())

        if (log) {
            logger.info("{} {} {} {}", logPrefix, request.uri(), response.statusCode(), response.body())
        } else {
            logger.debug("{} {} {} {}", logPrefix, request.uri(), response.statusCode(), response.body())
        }

        return if (response.statusCode() == 200) {
            response.body()
        } else if (nullStatuses.indexOf(response.statusCode()) != -1) {
            null
        } else {
            throw ApiException(logPrefix + " " + response.statusCode())
        }
    }

    private fun unauthenticatedGetRequest(path: String): HttpRequest {
        val method = "GET"

        logger.info("{} {}", path, method)

        return HttpRequest.newBuilder()
            .uri(URI.create(config.restUrl + path))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .header("Accept", "application/json")
            .timeout(ofSeconds(timeout)).build()
    }

    @Suppress("UastIncorrectHttpHeaderInspection")
    private fun authenticatedRequest(path: String, method: String, body: String, timestamp: String): HttpRequest {
        logger.info("{} {} {}", path, method, body)

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

    interface CoinbaseRestApiConfiguration {

        val restUrl: String
        val key: String
        val passphrase: String
        val secret: String

    }

}
