package allanlewis.products

import allanlewis.PositionConfig
import allanlewis.api.ApiException
import allanlewis.api.Product
import allanlewis.api.RestApi
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException

class ProductRepository(private val positionConfigs: Array<PositionConfig>, private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val products = HashMap<String, Product>()

    fun init(): ProductRepository {
        for (pc in positionConfigs) {
            logger.info("Loading {}", pc.id)

            try {
                val product = restApi.getProduct(pc.id)
                if (product != null) {
                    products[pc.id] = product
                } else {
                    logger.warn("{} not found", pc.id)
                }
            } catch (ex: ApiException) {
                val errMsg = "Unable to load $pc.id"
                logger.error(errMsg, ex)
                throw RuntimeException(errMsg, ex)
            }
        }

        return this
    }
    
    fun products(): Flux<Product> {
        return Flux.fromIterable(products.values)
    }

    fun product(id: String): Mono<Product> {
        return Mono.justOrEmpty(products[id])
    }

}
