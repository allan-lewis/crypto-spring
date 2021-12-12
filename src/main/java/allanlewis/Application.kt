package allanlewis

import allanlewis.api.RestApi
import allanlewis.products.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootApplication
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
@EnableConfigurationProperties(ConfigurationData::class)
open class ApplicationConfiguration(@Autowired private val restApi: RestApi,
                                    private val configurationData: ConfigurationData) {

    @Bean
    open fun productRepository(): ProductRepository {
        return ProductRepository(configurationData.positionConfigs, restApi).init()
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