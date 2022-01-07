@file:Suppress("unused")

package allanlewis.coinbase

import allanlewis.api.*
import allanlewis.utility.ToStringBuilder
import com.fasterxml.jackson.annotation.JsonProperty

data class CoinbaseProduct(@JsonProperty("id") override val id: String,
                           @JsonProperty("quote_increment") override val quoteIncrement: String,
                           @JsonProperty("base_increment") override val baseIncrement: String,
                           @JsonProperty("base_currency") val baseCurrency: String,
                           @JsonProperty("quote_currency") val quoteCurrency: String,
                           @JsonProperty("base_min_size") val baseMinSize: String,
                           @JsonProperty("base_max_size") val baseMaxSize: String,
                           @JsonProperty("display_name") val displayName: String,
                           @JsonProperty("min_market_funds") val minMarketFunds: String,
                           @JsonProperty("max_market_funds") val maxMarketFunds: String,
                           @JsonProperty("max_slippage_percentage") val maxSlippagePercentage: String,
                           @JsonProperty("margin_enabled") val marginEnabled: Boolean,
                           @JsonProperty("post_only") val postOnly: Boolean,
                           @JsonProperty("limit_only") val limitOnly: Boolean,
                           @JsonProperty("cancel_only") val cancelOnly: Boolean,
                           @JsonProperty("fx_stablecoin") val fxStablecoin: Boolean,
                           @JsonProperty("trading_disabled") val tradingDisabled: Boolean,
                           @JsonProperty("status") val status: String,
                           @JsonProperty("status_message") val statusMessage: String,
                           @JsonProperty("auction_mode") val auctionMode: Boolean): Product

class CoinbaseOrder: Order {

    @JsonProperty("id")
    override var id: String? = null

    @JsonProperty("client_oid")
    override var clientId: String? = null

    @JsonProperty("price")
    override var price: String? = null

    @JsonProperty("size")
    override var size: String? = null

    @JsonProperty("product_id")
    override var productId: String? = null

    @JsonProperty("profile_id")
    override var profileId: String? = null

    @JsonProperty("side")
    override var side: String? = null

    @JsonProperty("type")
    override var type: String? = null

    @JsonProperty("time_in_force")
    override var timeInForce: String? = null

    @JsonProperty("post_only")
    override var postOnly = false

    @JsonProperty("created_at")
    override var createdAt: String? = null

    @JsonProperty("fill_fees")
    override var fillFees: String? = null

    @JsonProperty("filled_size")
    override var filledSize: String? = null

    @JsonProperty("executed_value")
    override var executedValue: String? = null

    @JsonProperty("status")
    override var status: String? = null

    @JsonProperty("settled")
    override var settled = false

    @JsonProperty("stp")
    override var stop: String? = null

    @JsonProperty("funds")
    override var funds: String? = null

    @JsonProperty("specified_funds")
    override var specifiedFunds: String? = null

    @JsonProperty("done_at")
    override var doneAt: String? = null

    @JsonProperty("done_reason")
    override var doneReason: String? = null

    override fun toString(): String {
        return ToStringBuilder.toString(this)
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

    override fun toString(): String {
        return ToStringBuilder.toString(this)
    }

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

    override fun toString(): String {
        return ToStringBuilder.toString(this)
    }

}

class Channel {

    @JsonProperty("name")
    lateinit var type: String

    @JsonProperty("product_ids")
    lateinit var productIds: Array<String>

}

class CoinbaseAccount : Account {

    @JsonProperty("id")
    override var id: String? = null

    @JsonProperty("currency")
    override var currency: String? = null

    @JsonProperty("balance")
    override var balance: String? = null

    @JsonProperty("hold")
    var hold: String? = null

    @JsonProperty("available")
    var available: String? = null

    @JsonProperty("profile_id")
    var profileId: String? = null

    @JsonProperty("trading_enabled")
    var tradingEnabled: Boolean = false

    override fun toString(): String {
        return ToStringBuilder.toString(this)
    }

}