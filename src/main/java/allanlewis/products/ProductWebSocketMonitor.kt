package allanlewis.products

import allanlewis.api.WebSocketApi
import org.slf4j.LoggerFactory
import java.time.Duration

class ProductWebSocketMonitor(productRepository: ProductRepository, private val webSocketApi: WebSocketApi) {

    val logger = LoggerFactory.getLogger(javaClass)!!

    init {
        productRepository.products().map { p -> p.id }
            .subscribe { id -> webSocketApi.ticks(id).sample(Duration.ofSeconds(10)).subscribe { t ->
            if (t.stale()) {
                logger.warn("[tick_stale] {}", t)
            } else {
                logger.info("[tick_ok] {}", t)
            }
        }
        }
    }

}