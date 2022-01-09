package allanlewis.positions

import allanlewis.PositionConfig
import allanlewis.api.*
import allanlewis.coinbase.CoinbaseLimitOrder
import allanlewis.coinbase.CoinbaseMarketOrder
import allanlewis.coinbase.CoinbaseOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.ArgumentMatchers.*
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class PositionTest(@Mock private val product: Product,
                   @Mock private val restApi: RestApi,
                   @Mock private val orderFactory: OrderFactory) {

    private val config = PositionConfig("BTC-USD", 1, "10", ".005", ".99", "alwaysTrueStrategy")

    private val position = Position(config, product, OrderDone(restApi), OrderNotPending(restApi), orderFactory)

    private val marketOrder = CoinbaseMarketOrder("buy", "10", "BTC-USD", "", "")
    private val marketOrderPending = CoinbaseOrder("1", "pending", "", "", "market", "buy", "BTC-USD")
    private val marketOrderDone = CoinbaseOrder("1", "done", "filled", "10", "market", "buy", "BTC-USD")

    private val limitOrder = CoinbaseLimitOrder("buy", "10", "10", "BTC-USD", "", "")
    private val limitOrderPending = CoinbaseOrder("2", "pending", "", "", "limit", "buy", "BTC-USD")
    private val limitOrderOpen = CoinbaseOrder("2", "open", "", "", "limit", "buy", "BTC-USD")

    @BeforeEach
    fun setUp() {
        Mockito.`when`(product.id).thenReturn("BTC-USD")
        Mockito.`when`(product.quoteIncrement).thenReturn(".01")
        Mockito.`when`(product.baseIncrement).thenReturn(".00000001")

        Mockito.`when`(orderFactory.marketOrder(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(marketOrder)

        Mockito.`when`(orderFactory.limitOrder(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(limitOrder)

        Mockito.`when`(restApi.postOrder(marketOrder)).thenReturn(Mono.just(marketOrderPending))
        Mockito.`when`(restApi.postOrder(limitOrder)).thenReturn(Mono.just(limitOrderPending))
    }

    @Test
    fun happyPathPositionInit() {
        Mockito.`when`(restApi.getOrder("1")).thenReturn(Mono.just(marketOrderDone))
        Mockito.`when`(restApi.getOrder("2")).thenReturn(Mono.just(limitOrderOpen))

        StepVerifier.create(position.init())
            .expectNext(PositionState.Started)
            .expectNext(PositionState.BuyOrderPending)
            .expectNext(PositionState.BuyOrderFilled)
            .expectNext(PositionState.SellOrderPending)
            .expectNext(PositionState.SellOrderOpen)
            .verifyComplete()
    }

    @Test
    fun happyPathOrderDone() {
        Mockito.`when`(restApi.getOrder("1")).thenReturn(Mono.just(marketOrderDone))

        val exec = OrderDone(restApi)

        StepVerifier.create(exec.execute(marketOrder)).expectNext(marketOrderDone).verifyComplete()
    }

}