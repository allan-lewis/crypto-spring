@file:Suppress("unused")

package allanlewis.api

import com.fasterxml.jackson.annotation.JsonProperty

class Product {

    @JsonProperty(value = "id")
    var id: String? = null

    @JsonProperty(value = "base_currency")
    var baseCurrency: String? = null

    @JsonProperty(value = "quote_currency")
    var quoteCurrency: String? = null

    @JsonProperty(value = "base_min_size")
    var baseMinSize: String? = null

    @JsonProperty(value = "base_max_size")
    var baseMaxSize: String? = null

    @JsonProperty(value = "quote_increment")
    var quoteIncrement: String? = null

    @JsonProperty(value = "base_increment")
    var baseIncrement: String? = null

    @JsonProperty(value = "display_name")
    var displayName: String? = null

    @JsonProperty(value = "min_market_funds")
    var minMarketFunds: String? = null

    @JsonProperty(value = "max_market_funds")
    var maxMarketFunds: String? = null

    @JsonProperty(value = "max_slippage_percentage")
    var maxSlippagePercentage: String? = null

    @JsonProperty(value = "margin_enabled")
    var marginEnabled = false

    @JsonProperty(value = "post_only")
    var postOnly = false

    @JsonProperty(value = "limit_only")
    var limitOnly = false

    @JsonProperty(value = "cancel_only")
    var cancelOnly = false

    @JsonProperty(value = "fx_stablecoin")
    var fxStableCoin = false

    @JsonProperty(value = "trading_disabled")
    var tradingDisabled = false

    @JsonProperty(value = "status")
    var status: String? = null

    @JsonProperty(value = "status_message")
    var statusMessage: String? = null

    @JsonProperty(value = "auction_mode")
    var auctionMode = false

}

class Order {

    @JsonProperty("id")
    var id: String? = null

    @JsonProperty("price")
    var price: String? = null

    @JsonProperty("size")
    var size: String? = null

    @JsonProperty("product_id")
    var productId: String? = null

    @JsonProperty("profile_id")
    var profileId: String? = null

    @JsonProperty("side")
    var side: String? = null

    @JsonProperty("type")
    var type: String? = null

    @JsonProperty("time_in_force")
    var timeInForce: String? = null

    @JsonProperty("post_only")
    var postOnly = false

    @JsonProperty("created_at")
    var createdAt: String? = null

    @JsonProperty("fill_fees")
    var fillFees: String? = null

    @JsonProperty("filled_size")
    var filledSize: String? = null

    @JsonProperty("executed_value")
    var executedValue: String? = null

    @JsonProperty("status")
    var status: String? = null

    @JsonProperty("settled")
    var settled = false

    @JsonProperty("stp")
    var stop: String? = null

    @JsonProperty("funds")
    var funds: String? = null

    @JsonProperty("specified_funds")
    var specifiedFunds: String? = null

    @JsonProperty("done_at")
    var doneAt: String? = null

    @JsonProperty("done_reason")
    var doneReason: String? = null
}