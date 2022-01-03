package allanlewis.coinbase

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "coinbase")
data class CoinbaseConfigurationData(val profileId: String,
                                     val accountId: String,
                                     val restUrl: String,
                                     val webSocketUrl: String,
                                     val key: String,
                                     val passphrase: String,
                                     val secret: String)