package allanlewis

import allanlewis.api.Order
import allanlewis.api.Product
import allanlewis.api.RestApi
import allanlewis.api.WebSocketApi
import allanlewis.coinbase.*
import allanlewis.positions.OrderDone
import allanlewis.positions.OrderNotPending
import allanlewis.positions.Position
import allanlewis.positions.PositionManager
import allanlewis.products.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.web.reactive.socket.WebSocketHandler
import java.lang.IllegalArgumentException


@SpringBootApplication
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
@EnableConfigurationProperties(ConfigurationData::class, CoinbaseConfigurationData::class)
open class ApplicationConfiguration(private val configurationData: ConfigurationData,
                                    private val coinbaseConfigurationData: CoinbaseConfigurationData,
                                    private val applicationContext: ApplicationContext) {

    @Bean
    open fun productRepository(): ProductRepository {
        return ProductRepository(configurationData.positionConfigs, restApi()).init()
    }

    @Bean
    open fun positionManager(): PositionManager {
        return PositionManager(productRepository(), applicationContext).init()
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    open fun position(product: Product): Position {
        for (pc in configurationData.positionConfigs) {
            if (pc.id == product.id) {
                return Position(pc, product, applicationContext)
            }
        }

        throw IllegalArgumentException("No config for " + product.id)
    }

    @Bean
    open fun orderDone(): OrderDone? {
        return OrderDone(restApi())
    }

    @Bean
    open fun orderNotPending(): OrderNotPending? {
        return OrderNotPending(restApi())
    }

    @Bean
    open fun restApi(): RestApi {
        return CoinbaseRestApiImpl(coinbaseConfigurationData)
    }

    @Bean
    open fun webSocketApi(): WebSocketApi {
        return CoinbaseWebSocketApiImpl(coinbaseConfigurationData, webSocketHandler())
//        return CoinbaseWebSocketApiImpl(coinbaseConfigurationData, webSocketHandler()).init()
    }

    @Bean
    open fun webSocketHandler(): WebSocketHandler {
        return CoinbaseWebSocketHandler(coinbaseConfigurationData, productRepository())
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    open fun order(): Order {
        val order = CoinbaseOrder()
        order.profileId = coinbaseConfigurationData.profileId
        return order
    }

}

@ConstructorBinding
@ConfigurationProperties(prefix = "crypto.config")
data class ConfigurationData (val positionConfigs: Array<PositionConfig>)

data class PositionConfig(val id: String,
                          val max: Int,
                          val funds: String,
                          val fee: String,
                          val sell: String)