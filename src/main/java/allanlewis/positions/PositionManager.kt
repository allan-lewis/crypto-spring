package allanlewis.positions

import allanlewis.api.Product
import allanlewis.products.ProductRepository
import org.reactivestreams.Publisher
import org.springframework.context.ApplicationContext
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.util.concurrent.atomic.AtomicReference


class PositionManager(private val productRepository: ProductRepository, private val applicationContext: ApplicationContext) {

    private val positions = HashMap<String, Position>()

    fun init(): PositionManager {
        return this
    }

    fun newPosition(productId: String): Publisher<String> {
        val product = AtomicReference<Product>()
        return Mono.create { sink: MonoSink<String> ->
            run {
                productRepository.product(productId).subscribe({ p -> product.set(p) },
                    null,
                    {
                        if (product.get() != null) {
                            val position = applicationContext.getBean(Position::class.java, product.get())
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