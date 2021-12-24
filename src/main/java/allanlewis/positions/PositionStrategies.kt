package allanlewis.positions

import reactor.core.publisher.Mono

interface PositionStrategy {

    fun openPosition(productId: String): Mono<Boolean>

}

class AlwaysTrueStrategy : PositionStrategy {

    override fun openPosition(productId: String): Mono<Boolean> {
        return Mono.just(true)
    }

}

class DayRangeStrategy : PositionStrategy {

    override fun openPosition(productId: String): Mono<Boolean> {
        return Mono.just(false)
    }

}