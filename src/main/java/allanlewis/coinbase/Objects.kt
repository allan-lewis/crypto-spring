@file:Suppress("unused")

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

class SubscriptionMessage {

    @JsonProperty("signature")
    lateinit var signature: String

    @JsonProperty("key")
    lateinit var key: String

    @JsonProperty("passphrase")
    lateinit var passphrase: String

    @JsonProperty("timestamp")
    lateinit var timestamp: String

    @JsonProperty("product_ids")
    lateinit var productIds: Array<String>

    @JsonProperty("type")
    val type = "subscribe"

    @JsonProperty("channels")
    val channels = arrayOf("heartbeat", "user", "ticker")

}

class CoinbasePriceTick(override var price: String,
                        override var productId: String,
                        override var time: String,
                        override var twentyFourHourOpen: String,
                        override var twentyFourHourVolume: String,
                        override var twentyFourHourHigh: String,
                        override var twentyFourHourLow: String) : PriceTick {

}

class CoinbaseWebSocketMessage {

    @JsonProperty("type")
    lateinit var type: String

    @JsonProperty("channels")
    lateinit var channels: Array<Channel>

    @JsonProperty("sequence")
    var sequence: Long = 0

    @JsonProperty("product_id")
    lateinit var productId: String

    @JsonProperty("price")
    lateinit var price: String

    @JsonProperty("open_24h")
    lateinit var twentyFourHourOpen: String

    @JsonProperty("volume_24h")
    lateinit var twentyFourHourVolume: String

    @JsonProperty("high_24h")
    lateinit var twentyFourHourHigh: String

    @JsonProperty("low_24h")
    lateinit var twentyFourHourLow: String

    @JsonProperty("volume_30d")
    lateinit var thirtyDayVolume: String

    @JsonProperty("best_bid")
    lateinit var bestBid: String

    @JsonProperty("best_ask")
    lateinit var bestAsk: String

    @JsonProperty("side")
    lateinit var side: String

    @JsonProperty("time")
    lateinit var time: String

    @JsonProperty("trade_id")
    var tradeId: Long = 0

    @JsonProperty("last_size")
    lateinit var lastSize: String

    @JsonProperty("last_trade_id")
    var lastTradeId: Long = 0

    @JsonProperty("order_id")
    lateinit var orderId: String

    @JsonProperty("order_type")
    lateinit var orderType: String

    @JsonProperty("funds")
    lateinit var funds: String

    @JsonProperty("client_oid")
    lateinit var clientId: String

    @JsonProperty("profile_id")
    lateinit var profileId: String

    @JsonProperty("user_id")
    lateinit var userId: String

    @JsonProperty("reason")
    lateinit var reason: String

    @JsonProperty("maker_order_id")
    lateinit var makerOrderId: String

    @JsonProperty("remaining_size")
    lateinit var remainingSize: String

    @JsonProperty("taker_order_id")
    lateinit var takerOrderId: String

    @JsonProperty("taker_profile_id")
    lateinit var takerProfileId: String

    @JsonProperty("taker_user_id")
    lateinit var takerUserId: String

    @JsonProperty("taker_fee_rate")
    lateinit var takerFeeRate: String

    @JsonProperty("size")
    lateinit var size: String

    @JsonProperty("old_funds")
    lateinit var oldFunds: String

    @JsonProperty("new_funds")
    lateinit var newFunds: String

}

class Channel {

    @JsonProperty("name")
    lateinit var type: String

    @JsonProperty("product_ids")
    lateinit var productIds: Array<String>

}