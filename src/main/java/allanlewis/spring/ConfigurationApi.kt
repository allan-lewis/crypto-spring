package allanlewis.spring

import allanlewis.api.*
import allanlewis.positions.*
import allanlewis.products.ProductRepository
import allanlewis.products.ProductWebSocketMonitor
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope

@Configuration
@Import(ConfigurationCoinbase::class)
@EnableConfigurationProperties(ConfigurationData::class)
open class ApplicationConfiguration(private val configurationData: ConfigurationData,
                                    private val applicationContext: ApplicationContext) {

    @Bean
    open fun productRepository(): ProductRepository {
        return ProductRepository(configurationData.positionConfigs, restApi()).init()
    }

    @Bean
    open fun positionManager(): PositionManager {
        return PositionManager(productRepository(),
            configurationData.positionConfigs,
            positionFactory(),
            positionStrategyFactory(),
            restApi()).init()
    }

    @Bean
    open fun alwaysTrue(): AlwaysTrueStrategy {
        return AlwaysTrueStrategy()
    }

    @Bean
    open fun alwaysFalse(): AlwaysFalseStrategy {
        return AlwaysFalseStrategy()
    }

    @Bean
    open fun dayRange(): DayRangeStrategy {
        return DayRangeStrategy(configurationData.positionConfigs, productRepository(), restApi(), webSocketApi()).init()
    }

    @Bean
    open fun alwaysTrueWithFunds(): AlwaysTrueWithFunds {
        return AlwaysTrueWithFunds(configurationData.positionConfigs, productRepository(), restApi())
    }

    @Bean
    open fun positionFactory(): PositionFactory {
        return object : PositionFactory {

            override fun position(product: Product): Position {
                for (pc in configurationData.positionConfigs) {
                    if (pc.id == product.id) {
                        return Position(pc, product, orderDone()!!, orderNotPending()!!, orderFactory())
                    }
                }

                throw IllegalArgumentException("No config for " + product.id)
            }

        }
    }

    @Bean
    open fun positionStrategyFactory(): PositionStrategyFactory {
        return object : PositionStrategyFactory {

            override fun strategy(name: String): PositionStrategy {
                return applicationContext.getBean(name, PositionStrategy::class.java)
            }

        }
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    open fun orderDone(): OrderDone? {
        return OrderDone(restApi(), 10)
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    open fun orderNotPending(): OrderNotPending? {
        return OrderNotPending(restApi(), 10)
    }

    @Bean
    open fun webSocketHandler(): DefaultWebSocketHandler {
        return DefaultWebSocketHandler(webSocketApiImpl(), productRepository()).init()
    }

    @Bean
    open fun webSocketClient(): DefaultWebSocketClient {
        return DefaultWebSocketClient(webSocketApiImpl().url, webSocketHandler()).init()
    }

    private fun restApi(): RestApi {
        return applicationContext.getBean(RestApi::class.java)
    }

    private fun webSocketApi(): WebSocketApi {
        return applicationContext.getBean(WebSocketApi::class.java)
    }

    private fun orderFactory(): OrderFactory {
        return applicationContext.getBean(OrderFactory::class.java)
    }

    private fun webSocketApiImpl(): WebSocketApiImpl {
        return applicationContext.getBean(WebSocketApiImpl::class.java)
    }

    @Bean
    open fun productWebSocketMonitor(): ProductWebSocketMonitor {
        return ProductWebSocketMonitor(productRepository(), webSocketApi())
    }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "api")
data class ConfigurationData (val positionConfigs: Array<PositionConfig>)
