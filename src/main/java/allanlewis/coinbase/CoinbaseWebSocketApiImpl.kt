package allanlewis.coinbase

import allanlewis.api.WebSocketApi
import allanlewis.coinbase.CoinbaseUtilities.sign
import allanlewis.coinbase.CoinbaseUtilities.timestamp
import allanlewis.products.ProductRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.StandardWebSocketClient
import org.springframework.web.socket.WebSocketHttpHeaders
import reactor.core.publisher.Mono
import java.net.URI
import java.util.logging.Level
import javax.websocket.ContainerProvider

class CoinbaseWebSocketApiImpl(private val config: CoinbaseConfigurationData,
                               private val webSocketHandler: WebSocketHandler): WebSocketApi {

    private val size = 2048000

    fun init(): CoinbaseWebSocketApiImpl {
        val container = ContainerProvider.getWebSocketContainer()
        container.defaultMaxTextMessageBufferSize = size
        container.defaultMaxBinaryMessageBufferSize = size

        val webSocketClient = StandardWebSocketClient(container)
        val headers = WebSocketHttpHeaders()
        val uri = URI.create(config.webSocketUrl)
        webSocketClient.execute(uri, headers, webSocketHandler).subscribe()

        return this
    }

}

class CoinbaseWebSocketHandler(private val config: CoinbaseConfigurationData,
                               private val productRepository: ProductRepository) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val timestamp = timestamp()
        val signature = sign(config.secret, "/users/self/verify", "GET", "", timestamp)

        val ids = ArrayList<String>()
        productRepository.products().mapNotNull { p -> ids.add(p.id!!) }.blockLast()

        val message = SubscriptionMessage ()
        message.signature = signature
        message.key = config.key
        message.passphrase = config.passphrase
        message.timestamp = timestamp
        message.productIds = ids.toTypedArray()

        val payload =  ObjectMapper().writeValueAsString(message)

        return session.send(Mono.just(payload)
            .map(session::textMessage))
            .and(session.receive().map(WebSocketMessage::getPayloadAsText).log())
            .and(session.closeStatus().map(CloseStatus::getCode).log())
    }

}
