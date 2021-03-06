package allanlewis.products

import allanlewis.api.PositionConfig
import allanlewis.api.Product
import allanlewis.api.RestApi
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

class ProductRepository(private val positionConfigs: Array<PositionConfig>, private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val products = ConcurrentHashMap<String, Mono<Product>>()

    fun init(): ProductRepository {
        for (pc in positionConfigs) {
            logger.info("Loading product {}", pc)

            products[pc.id] = restApi.getProduct(pc.id)
        }

        return this
    }

    fun products(): Flux<Product> {
        return Flux.merge(products.values)
    }

    fun product(id: String): Mono<Product> {
        return if (products[id] == null) Mono.empty() else products[id]!!
    }

}
