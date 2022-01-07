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

    @GetMapping
    fun get(): Publisher<PositionJson> {
        return positionManager.getPositions()
    }

    @GetMapping(path = ["/{positionId}"])
    fun get(@PathVariable positionId: String): Publisher<PositionJson> {
        return positionManager.getPosition(positionId)
    }

}
