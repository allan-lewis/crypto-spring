package allanlewis.coinbase

import allanlewis.api.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

@JsonDeserialize(using = CoinbaseAccountDeserializer::class)
data class CoinbaseAccount(@JsonProperty("id") override val id: String,
                           @JsonProperty("currency") override val currency: String,
                           @JsonProperty("balance") override val balance: String) : Account

@JsonDeserialize(using = CoinbaseProductDeserializer::class)
data class CoinbaseProduct(@JsonProperty("id") override val id: String,
                           @JsonProperty("quote_increment") override val quoteIncrement: String,
                           @JsonProperty("base_increment") override val baseIncrement: String,
                           @JsonProperty("base_currency") override val baseCurrency: String,
                           @JsonProperty("quote_currency") override val quoteCurrency: String,): Product

abstract class CoinbaseBaseOrder(override val type: String,
                                 override val side: String,
                                 override val productId: String): BaseOrder

@JsonDeserialize(using = CoinbaseOrderDeserializer::class)
data class CoinbaseOrder(@JsonProperty("id") override val id: String,
                         @JsonProperty("status") override val status: String,
                         @JsonProperty("done_reason") override val doneReason: String,
                         @JsonProperty("filled_size") override val filledSize: String,
                         @JsonProperty("type") override val type: String,
                         @JsonProperty("side") override val side: String,
                         @JsonProperty("product_id") override val productId: String) : CoinbaseBaseOrder(type, side, productId), ReadOrder

abstract class CoinbaseWriteOrder(override val type: String,
                                  override val side: String,
                                  override val productId: String,
                                  override val profileId: String,
                                  override val clientId: String): CoinbaseBaseOrder(type, side, productId), WriteOrder

data class CoinbaseMarketOrder(@JsonProperty("side") override val side: String,
                               @JsonProperty("funds") override val funds: String,
                               @JsonProperty("product_id") override val productId: String,
                               @JsonProperty("profile_id") override val profileId: String,
                               @JsonProperty("client_oid") override val clientId: String): CoinbaseWriteOrder("market", side, productId, profileId, clientId), MarketOrder

data class CoinbaseLimitOrder(@JsonProperty("side") override val side: String,
                              @JsonProperty("price") override val price: String,
                              @JsonProperty("size") override val size: String,
                              @JsonProperty("product_id") override val productId: String,
                              @JsonProperty("profile_id") override val profileId: String,
                              @JsonProperty("client_oid") override val clientId: String): CoinbaseWriteOrder("limit", side, productId, profileId, clientId), LimitOrder

private class CoinbaseOrderDeserializer: StdDeserializer<CoinbaseOrder>(CoinbaseOrder::class.java) {

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): CoinbaseOrder {
        val node: JsonNode = parser!!.codec.readTree(parser)

        val id = node.get("id").asText()
        val status = node.get("status").asText()
        val doneReason = if (node.get("done_reason") == null) "" else node.get("done_reason").asText()
        val filledSize = if (node.get("filled_size") == null) "" else node.get("filled_size").asText()
        val side = node.get("side").asText()
        val type = node.get("type").asText()
        val productId = node.get("product_id").asText()

        return CoinbaseOrder(id, status, doneReason, filledSize, type, side, productId)
    }

}

private class CoinbaseProductDeserializer: StdDeserializer<CoinbaseProduct>(CoinbaseProduct::class.java) {

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): CoinbaseProduct {
        val node: JsonNode = parser!!.codec.readTree(parser)

        val id = node.get("id").asText()
        val quoteIncrement = node.get("quote_increment").asText()
        val baseIncrement = node.get("base_increment").asText()
        val baseCurrency = node.get("base_currency").asText()
        val quoteCurrency = node.get("quote_currency").asText()

        return CoinbaseProduct(id, quoteIncrement, baseIncrement, baseCurrency, quoteCurrency)
    }

}

private class CoinbaseAccountDeserializer: StdDeserializer<CoinbaseAccount>(CoinbaseProduct::class.java) {

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): CoinbaseAccount {
        val node: JsonNode = parser!!.codec.readTree(parser)

        val id = node.get("id").asText()
        val currency = node.get("quote_increment").asText()
        val balance = node.get("base_increment").asText()

        return CoinbaseAccount(id, currency, balance)
    }

}

data class SubscriptionMessage(@JsonProperty("signature") val signature: String,
                               @JsonProperty("key") val key: String,
                               @JsonProperty("passphrase") val passphrase: String,
                               @JsonProperty("timestamp") val timestamp: String,
                               @JsonProperty("product_ids") val productIds: Array<String>) {

    @JsonProperty("type")
    val type = "subscribe"

    @JsonProperty("channels")
    val channels = arrayOf("heartbeat", "user", "ticker")

}

data class CoinbasePriceTick(override val price: String,
                             override val productId: String,
                             override val time: String,
                             override val twentyFourHourOpen: String,
                             override val twentyFourHourVolume: String,
                             override val twentyFourHourHigh: String,
                             override val twentyFourHourLow: String) : PriceTick

@JsonDeserialize(using = CoinbaseWebSocketMessageDeserializer::class)
data class CoinbaseWebSocketMessage(val type: String,
                                    val productId: String,
                                    val time: String,
                                    val price: String,
                                    val twentyFourHourOpen: String,
                                    val twentyFourHourVolume: String,
                                    val twentyFourHourHigh: String,
                                    val twentyFourHourLow: String)

private class CoinbaseWebSocketMessageDeserializer: StdDeserializer<CoinbaseWebSocketMessage>(CoinbaseWebSocketMessage::class.java) {

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): CoinbaseWebSocketMessage {
        val node: JsonNode = parser!!.codec.readTree(parser)

        val type = node.get("type").asText()
        val productId = if (node.get("product_id") == null) "" else node.get("product_id").asText()
        val time = if (node.get("time") == null) "" else node.get("time").asText()
        val price = if (node.get("price") == null) "" else node.get("price").asText()
        val twentyFourHourOpen = if (node.get("open_24h") == null) "" else node.get("open_24h").asText()
        val twentyFourHourVolume = if (node.get("volume_24h") == null) "" else node.get("volume_24h").asText()
        val twentyFourHourHigh = if (node.get("high_24h") == null) "" else node.get("high_24h").asText()
        val twentyFourHourLow = if (node.get("low_24h") == null) "" else node.get("low_24h").asText()

        return CoinbaseWebSocketMessage(type,
            productId,
            time,
            price,
            twentyFourHourOpen,
            twentyFourHourVolume,
            twentyFourHourHigh,
            twentyFourHourLow)
    }

}