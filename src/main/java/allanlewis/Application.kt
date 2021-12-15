package allanlewis

import allanlewis.api.Product
import allanlewis.api.RestApi
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
import org.springframework.context.annotation.Scope
import java.lang.IllegalArgumentException


@SpringBootApplication
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
@EnableConfigurationProperties(ConfigurationData::class)
open class ApplicationConfiguration(@Autowired private val restApi: RestApi,
                                    private val configurationData: ConfigurationData,
                                    private val applicationContext: ApplicationContext) {

    @Bean
    open fun productRepository(): ProductRepository {
        return ProductRepository(configurationData.positionConfigs, restApi).init()
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
        return OrderDone(restApi)
    }

    @Bean
    open fun orderNotPending(): OrderNotPending? {
        return OrderNotPending(restApi)
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