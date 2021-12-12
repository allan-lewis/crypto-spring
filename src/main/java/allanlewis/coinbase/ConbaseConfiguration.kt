package allanlewis.coinbase

import allanlewis.api.RestApi
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(CoinbaseConfigurationData::class)
open class CoinbaseConfiguration(private val configurationData: CoinbaseConfigurationData) {

    @Bean
    open fun restApi(): RestApi {
        return CoinbaseRestApiImpl(configurationData)
    }

}

@ConstructorBinding
@ConfigurationProperties(prefix = "coinbase")
data class CoinbaseConfigurationData(val profileId: String, val restUrl: String)