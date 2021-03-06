package allanlewis.spring

import allanlewis.api.Product
import allanlewis.api.ReadOrder
import allanlewis.api.RestApi
import allanlewis.positions.PositionManager
import allanlewis.positions.PositionSummary
import allanlewis.products.ProductRepository
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/products")
class ProductsController(@Autowired private val productRepository: ProductRepository) {

    @GetMapping
    fun get(): Publisher<Product> {
        return productRepository.products()
    }

    @GetMapping(path = ["/{id}"])
    fun get(@PathVariable id: String): Publisher<Product> {
        return Mono.from(productRepository.product(id))
    }

}

@RestController
@RequestMapping(path = ["/positions"])
class PositionsController(@Autowired private val positionManager: PositionManager) {

    @PostMapping(path = ["/{productId}"])
    fun post(@PathVariable productId: String): Publisher<String> {
        return positionManager.newPosition(productId)
    }

    @GetMapping
    fun get(): Publisher<PositionSummary> {
        return positionManager.getPositions()
    }

    @GetMapping(path = ["/{positionId}"])
    fun get(@PathVariable positionId: String): Publisher<PositionSummary> {
        return positionManager.getPosition(positionId)
    }

}

@RestController
@RequestMapping(path = ["/orders"])
class OrdersController(@Autowired private val restApi: RestApi) {

    @GetMapping(path = ["/{id}"])
    fun get(@PathVariable id: String): Publisher<ReadOrder> {
        return restApi.getOrder(id)
    }

}