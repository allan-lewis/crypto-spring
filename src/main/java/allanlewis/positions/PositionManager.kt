package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.Order
import allanlewis.api.Product
import allanlewis.api.RestApi
import allanlewis.products.ProductRepository
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

class PositionManager(private val productRepository: ProductRepository,
                      private val positionConfigs: Array<PositionConfig>,
                      private val positionFactory: PositionFactory,
                      private val positionStrategyFactory: PositionStrategyFactory,
                      private val restApi: RestApi) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val positions = ConcurrentHashMap<String, Position>()
    private val intervalSeconds = 60L

    fun init(): PositionManager {
        for (pc in positionConfigs) {
            logger.info("Initializing {}", pc.id)
            productRepository.product(pc.id).subscribe { p -> manage(p, pc) }
        }
        
        return this
    }

    private fun manage(product: Product, config: PositionConfig) {
        logger.info("Managing {}", product.id)
        Flux.interval(Duration.ofSeconds(intervalSeconds)).subscribe {
            val orders = ArrayList<Order>()
            restApi.getOrders().subscribe({orders.add(it)},
                null,
                {
                    val orderCount = orders.filter { it.productId == product.id }.size

                    if (orderCount < config.max) {
                        logger.info("For {} order count is {} (max {}), considering opening a position", product.id, orderCount, config.max)
                        positionStrategyFactory.strategy(config.strategy)
                            .openPosition(product.id!!)
                            .log()
                            .subscribe { b -> if (b) newPosition(product).log().subscribe() }
                    } else{
                        logger.info("Nothing to do for {}, order count {} (max {})", product.id, orderCount, config.max)
                    }
                })
        }
    }

    fun newPosition(productId: String): Mono<String> {
        return productRepository.product(productId).flatMap { p -> newPosition(p) }
    }

    private fun newPosition(product: Product): Mono<String> {
        val position = positionFactory.position(product)

        logger.info("Opening a new position for {}  {}", product.id, position.id)

        position.init()
        positions[position.id] = position

        return Mono.just(position.id)
    }

    fun getPosition(transactionId: String?): Publisher<Position> {
        return Mono.justOrEmpty(positions[transactionId])
    }

}