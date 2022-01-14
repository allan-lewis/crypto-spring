package allanlewis.spring

import allanlewis.api.*
import allanlewis.coinbase.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.socket.WebSocketHandler

@Configuration
@EnableConfigurationProperties(ConfigurationDataCoinbase::class)
open class ConfigurationCoinbase(private val configurationDataCoinbase: ConfigurationDataCoinbase) {

    @Bean
    open  fun restApi(): RestApi {
        return CoinbaseRestApiImpl(configurationDataCoinbase)
    }

    @Bean
    open fun webSocketApi(): CoinbaseWebSocketApiImpl {
        return CoinbaseWebSocketApiImpl(configurationDataCoinbase)
    }

    @Bean
    open fun webSocketHandler(): WebSocketHandler {
        return DefaultWebSocketHandler(webSocketApi())
    }

    @Bean
    open fun webSocketClient(): DefaultWebSocketClient {
        return DefaultWebSocketClient(configurationDataCoinbase.webSocketUrl, webSocketHandler()).init()
    }

    @Bean
    open fun orderFactory(): OrderFactory {

        return object : OrderFactory {

            override fun marketOrder(productId: String, side: String, funds: String, clientId: String): MarketOrder {
                return CoinbaseMarketOrder(side, funds, productId, configurationDataCoinbase.profileId, clientId)
            }

            override fun limitOrder(productId: String, side: String, price: String, size: String, clientId: String): LimitOrder {
                return CoinbaseLimitOrder(side, price, size, productId, configurationDataCoinbase.profileId, clientId)
            }

        }
    }

}

@ConstructorBinding
@ConfigurationProperties(prefix = "coinbase")
data class ConfigurationDataCoinbase(val profileId: String,
                                     override val restUrl: String,
                                     override val webSocketUrl: String,
                                     override val key: String,
                                     override val passphrase: String,
                                     override val secret: String): CoinbaseWebSocketConfiguration