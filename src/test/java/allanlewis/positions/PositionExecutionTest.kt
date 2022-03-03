package allanlewis.positions

import allanlewis.api.RestApi
import allanlewis.coinbase.CoinbaseLimitOrder
import allanlewis.coinbase.CoinbaseOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@Suppress("ReactiveStreamsUnusedPublisher")
class PositionExecutionTest(@Mock private val restApi: RestApi) {

    private val orderDone = OrderDone(restApi, 3)
    private val limitOrder = CoinbaseLimitOrder("sell", "50000", ".001", "BTC-USD", "pid", "cid", true)
    private val limitOrderPending = CoinbaseOrder("2", "pending", "", "", "limit", "buy", "BTC-USD")
    private val limitOrderDone = CoinbaseOrder("2", "done", "", "", "limit", "buy", "BTC-USD")
    private var count = 0

    @BeforeEach
    fun setup() {
        count = 0
        Mockito.`when`(restApi.postOrder(limitOrder)).thenReturn(Mono.just(limitOrderPending))
    }

    @Test
    fun orderDoneHappyPath() {
        Mockito.`when`(restApi.getOrder("2")).thenReturn(Mono.just(limitOrderDone))

        StepVerifier.create(orderDone.execute(limitOrder))
            .expectNext(limitOrderDone)
            .verifyComplete()
    }

    @Test
    fun orderDoneNeverFound() {
        Mockito.`when`(restApi.getOrder("2")).thenReturn(Mono.empty())

        StepVerifier.create(orderDone.execute(limitOrder))
            .expectNextCount(0)
            .verifyErrorMessage("Retries exhausted: 3/3")

        Mockito.verify(restApi, Mockito.times(4)).getOrder(anyString())
    }

    @Test
    fun orderDoneNeverDone() {
        Mockito.`when`(restApi.getOrder("2")).thenReturn(Mono.just(limitOrderPending))

        StepVerifier.create(orderDone.execute(limitOrder))
            .expectNextCount(0)
            .verifyErrorMessage("Retries exhausted: 3/3")

        Mockito.verify(restApi, Mockito.times(4)).getOrder(anyString())
    }

    @Test
    fun orderDoneEventuallyDone() {
        Mockito.`when`(restApi.getOrder("2")).then {
            when (count++) {
                0 -> Mono.empty()
                1 -> Mono.just(limitOrderPending)
                else -> Mono.just(limitOrderDone)
            }
        }

        StepVerifier.create(orderDone.execute(limitOrder))
            .expectNext(limitOrderDone)
            .verifyComplete()

        Mockito.verify(restApi, Mockito.times(3)).getOrder(anyString())
    }

}