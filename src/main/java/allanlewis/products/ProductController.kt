package allanlewis.products

import allanlewis.api.Product
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/products")
class ProductController(@Autowired private val productRepository: ProductRepository) {

    @GetMapping
    fun get(): Publisher<Product> {
        return productRepository.products()
    }

    @GetMapping(path = ["/{id}"])
    fun get(@PathVariable id: String): Publisher<Product> {
        return Mono.from(productRepository.product(id))
    }

}