package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.Product
import allanlewis.orders.OrderBook
import allanlewis.products.ProductRepository
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

class PositionManager(private val productRepository: ProductRepository,
                      private val positionConfigs: Array<PositionConfig>,
                      private val orderBook: OrderBook,
                      private val applicationContext: ApplicationContext) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val positions = HashMap<String, Position>()
    private val intervalSeconds = 10L

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
            val orderCount = orderBook.orders().filter { o -> o.productId == product.id }.size

            if (orderCount < config.max) {
                logger.info("For {} order count is {} (max {}), considering opening a position", product.id, orderCount, config.max)
                applicationContext.getBean(config.strategy, PositionStrategy::class.java)
                    .openPosition(product.id!!)
                    .log()
                    .subscribe { b -> if (b) newPosition(product).log().subscribe() }
            } else{
                logger.info("Nothing to do for {}, order count {} (max {})", product.id, orderCount, config.max)
            }
        }
    }

    fun newPosition(product: Product): Mono<String> {
        val position = applicationContext.getBean(Position::class.java, product)

        logger.info("Opening a new {} position for {}", position.id, product.id)

        return Mono.just(position.id)
    }

    fun getPosition(transactionId: String?): Publisher<Position> {
        return Mono.justOrEmpty(positions[transactionId])
    }

}