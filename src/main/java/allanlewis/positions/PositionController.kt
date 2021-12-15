package allanlewis.positions

import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(path = ["/positions"])
class PositionController(@Autowired private val positionManager: PositionManager) {

    @PostMapping(path = ["/{productId}"])
    fun post(@PathVariable productId: String): Publisher<String> {
        return positionManager.newPosition(productId)
    }

    @GetMapping(path = ["/{transactionId}"])
    operator fun get(@PathVariable transactionId: String?): Publisher<Position> {
        return positionManager.getPosition(transactionId)
    }

}