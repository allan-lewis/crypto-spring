package allanlewis.positions

import allanlewis.api.*
import allanlewis.coinbase.CoinbaseMarketOrder
import allanlewis.coinbase.CoinbaseOrder
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PositionExecutionTest {

    private val marketOrder = CoinbaseMarketOrder("buy", "10", "BTC-USD", "profileId", "clientId")
    private val pendingOrder = CoinbaseOrder("1", "pending", "", "", "", "", "BTC-USD")
    private val doneOrder = CoinbaseOrder("1", "done", "filled", "", "", "", "BTC-USD")

    @Test
    fun happyPathOrderDone() {
        val restApi: RestApi = object: RestApi {
            override fun getProduct(id: String): Mono<Product> {
                TODO("Not yet implemented")
            }

            override fun getOrder(id: String): Mono<ReadOrder> {
                return Mono.just(doneOrder)
            }

            override fun getOrders(): Flux<ReadOrder> {
                TODO("Not yet implemented")
            }

            override fun postOrder(order: WriteOrder): Mono<ReadOrder> {
                return Mono.just(pendingOrder)
            }

            override fun getAccounts(): Flux<Account> {
                TODO("Not yet implemented")
            }

            override fun getAccount(id: String): Mono<Account> {
                TODO("Not yet implemented")
            }

        }

        val exec = OrderDone(restApi)

        StepVerifier.create(exec.execute(marketOrder)).expectNext(doneOrder).verifyComplete()
    }

}