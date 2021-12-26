package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.Product
import allanlewis.api.RestApi
import allanlewis.products.ProductRepository
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class PositionManager(private val productRepository: ProductRepository,
                      private val positionConfigs: Array<PositionConfig>,
                      private val restApi: RestApi,
                      private val applicationContext: ApplicationContext) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val positions = HashMap<String, Position>()
    private val intervalSeconds = 15L

    fun init(): PositionManager {
        productRepository.products().subscribe { p -> manage(p) }
        return this
    }

    private fun manage(product: Product) {
        logger.info("Managing positions for {}", product.id)
        Flux.interval(Duration.ofSeconds(intervalSeconds)).subscribe {
            run {
                var max = 0
                for (pc in positionConfigs) {
                    if (pc.id == product.id) {
                        max = pc.max
                    }
                }
                val orderCount = restApi.getOrders().filter { o -> o.productId == product.id }.size
                if (orderCount < max) {
                    logger.info("For {} order count is {} (max {}), considering opening a position", product.id, orderCount, max)
                    applicationContext.getBean(PositionStrategy::class.java)
                            .openPosition(product.id!!)
                            .log()
                            .subscribe { b -> if (b) newPosition(product.id!!).log().subscribe() }
                } else{
                    logger.info("Nothing to do for {}, order count {} (max {})", product.id, orderCount, max)
                }
            }
        }
    }

    fun newPosition(productId: String): Mono<String> {
        logger.info("Opening a new position for {}", productId)

        val product = AtomicReference<Product>()
        return Mono.create { sink: MonoSink<String> ->
            run {
                productRepository.product(productId).subscribe({ p -> product.set(p) },
                    null,
                    {
                        if (product.get() != null) {
                            val position = applicationContext.getBean(Position::class.java, product.get())

                            logger.info("Created position {} {} for {}", position.hashCode(), position.id, product.get().id)

                            positions[position.id] = position.init()
                            sink.success(position.id)
                        } else {
                            sink.error(IllegalArgumentException("Product $productId not found"))
                        }
                    })
            }
        }
    }

    fun getPosition(transactionId: String?): Publisher<Position> {
        val transaction = positions[transactionId]
        return if (transaction != null) Mono.just(transaction) else Mono.empty()
    }

}