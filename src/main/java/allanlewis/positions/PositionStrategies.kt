package allanlewis.positions

import reactor.core.publisher.Mono

interface PositionStrategy {

    fun openPosition(): Mono<Boolean>

}

class AlwaysTrueStrategy : PositionStrategy {

    override fun openPosition(): Mono<Boolean> {
        return Mono.just(true)
    }

}