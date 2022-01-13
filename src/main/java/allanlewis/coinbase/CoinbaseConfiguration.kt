package allanlewis.coinbase

import allanlewis.api.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(CoinbaseConfigurationData::class)
open class CoinbaseConfiguration(private val coinbaseConfigurationData: CoinbaseConfigurationData) {

    @Bean
    open  fun restApi(): RestApi {
        return CoinbaseRestApiImpl(coinbaseConfigurationData)
    }

    @Bean
    open fun webSocketApi(): WebSocketApi {
        return CoinbaseWebSocketApiImpl(coinbaseConfigurationData, webSocketHandler()).init()
    }

    @Bean
    open fun webSocketHandler(): CoinbaseWebSocketHandler {
        return CoinbaseWebSocketHandler(coinbaseConfigurationData)
    }

    @Bean
    open fun orderFactory(): OrderFactory {

        return object : OrderFactory {

            override fun marketOrder(productId: String, side: String, funds: String, clientId: String): MarketOrder {
                return CoinbaseMarketOrder(side, funds, productId, coinbaseConfigurationData.profileId, clientId)
            }

            override fun limitOrder(productId: String, side: String, price: String, size: String, clientId: String): LimitOrder {
                return CoinbaseLimitOrder(side, price, size, productId, coinbaseConfigurationData.profileId, clientId)
            }

        }
    }

}

@ConstructorBinding
@ConfigurationProperties(prefix = "coinbase")
data class CoinbaseConfigurationData(val profileId: String,
                                     val restUrl: String,
                                     val webSocketUrl: String,
                                     val key: String,
                                     val passphrase: String,
                                     val secret: String)