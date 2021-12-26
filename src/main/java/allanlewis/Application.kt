package allanlewis

import allanlewis.api.Order
import allanlewis.api.Product
import allanlewis.api.RestApi
import allanlewis.api.WebSocketApi
import allanlewis.coinbase.*
import allanlewis.positions.*
import allanlewis.products.ProductRepository
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.apache.log4j.Logger
import org.slf4j.bridge.SLF4JBridgeHandler
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J


@SpringBootApplication
open class Application: InitializingBean {

    override fun afterPropertiesSet() {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
        println("logged via system.out")
        System.err.println("logged via system.err")

        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
        val julLogger = java.util.logging.Logger.getLogger(Application::class.java.name)
        julLogger.info("logged via jul")

        val log4JLogger: Logger = Logger.getLogger(Application::class.java)
        log4JLogger.info("logged via log4j")

        val jclLogger: Log = LogFactory.getLog(Application::class.java)
        jclLogger.info("logged via jcl")
    }

}

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
        return PositionManager(productRepository(),
                configurationData.positionConfigs,
                restApi(),
                applicationContext).init()
    }

    @Bean()
    open fun positionStrategy(): PositionStrategy {
        return DayRangeStrategy(webSocketApi()).init()
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
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    open fun orderDone(): OrderDone? {
        return OrderDone(restApi())
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    open fun orderNotPending(): OrderNotPending? {
        return OrderNotPending(restApi())
    }

    @Bean
    open fun restApi(): RestApi {
        return CoinbaseRestApiImpl(coinbaseConfigurationData)
    }

    @Bean
    open fun webSocketApi(): WebSocketApi {
        return CoinbaseWebSocketApiImpl(coinbaseConfigurationData, webSocketHandler()).init()
    }

    @Bean
    open fun webSocketHandler(): CoinbaseWebSocketHandler {
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