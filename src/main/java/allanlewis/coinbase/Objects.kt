@file:Suppress("unused")

package allanlewis.coinbase

import allanlewis.api.Order
import allanlewis.api.Product
import com.fasterxml.jackson.annotation.JsonProperty

class CoinbaseProduct: Product {

    @JsonProperty(value = "id")
    override var id: String? = null

    @JsonProperty(value = "base_currency")
    override var baseCurrency: String? = null

    @JsonProperty(value = "quote_currency")
    override var quoteCurrency: String? = null

    @JsonProperty(value = "base_min_size")
    override var baseMinSize: String? = null

    @JsonProperty(value = "base_max_size")
    override var baseMaxSize: String? = null

    @JsonProperty(value = "quote_increment")
    override var quoteIncrement: String? = null

    @JsonProperty(value = "base_increment")
    override var baseIncrement: String? = null

    @JsonProperty(value = "display_name")
    override var displayName: String? = null

    @JsonProperty(value = "min_market_funds")
    override var minMarketFunds: String? = null

    @JsonProperty(value = "max_market_funds")
    override var maxMarketFunds: String? = null

    @JsonProperty(value = "max_slippage_percentage")
    override var maxSlippagePercentage: String? = null

    @JsonProperty(value = "margin_enabled")
    override var marginEnabled = false

    @JsonProperty(value = "post_only")
    override var postOnly = false

    @JsonProperty(value = "limit_only")
    override var limitOnly = false

    @JsonProperty(value = "cancel_only")
    override var cancelOnly = false

    @JsonProperty(value = "fx_stablecoin")
    override var fxStableCoin = false

    @JsonProperty(value = "trading_disabled")
    override var tradingDisabled = false

    @JsonProperty(value = "status")
    override var status: String? = null

    @JsonProperty(value = "status_message")
    override var statusMessage: String? = null

    @JsonProperty(value = "auction_mode")
    override var auctionMode = false

}

class CoinbaseOrder: Order {

    @JsonProperty("id")
    override var id: String? = null

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
}