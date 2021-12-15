package allanlewis.coinbase

import allanlewis.api.RestApi
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
@EnableConfigurationProperties(CoinbaseConfigurationData::class)
open class CoinbaseConfiguration(private val configurationData: CoinbaseConfigurationData) {

    @Bean
    open fun restApi(): RestApi {
        return CoinbaseRestApiImpl(configurationData)
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    open fun order(): CoinbaseOrder {
        val order = CoinbaseOrder()
        order.profileId = configurationData.profileId
        return order
    }

}

@ConstructorBinding
@ConfigurationProperties(prefix = "coinbase")
data class CoinbaseConfigurationData(val profileId: String,
                                     val restUrl: String,
                                     val key: String,
                                     val passphrase: String,
                                     val secret: String)