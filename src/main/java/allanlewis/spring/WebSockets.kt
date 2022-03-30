package allanlewis.spring

import allanlewis.api.WebSocketApiImpl
import allanlewis.products.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.StandardWebSocketClient
import reactor.core.publisher.Mono
import java.net.URI
import javax.websocket.ContainerProvider

class DefaultWebSocketClient(private val webSocketUrl: String, private val webSocketHandler: WebSocketHandler) {

    private val size = 2048000

    fun init(): DefaultWebSocketClient {
        val container = ContainerProvider.getWebSocketContainer()
        container.defaultMaxTextMessageBufferSize = size
        container.defaultMaxBinaryMessageBufferSize = size

        val webSocketClient = StandardWebSocketClient(container)
        val uri = URI.create(webSocketUrl)
        webSocketClient.execute(uri, webSocketHandler).subscribe()

        return this
    }

}

class DefaultWebSocketHandler(private val webSocketApiImpl: WebSocketApiImpl,
                              private val productRepository: ProductRepository): WebSocketHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handle(session: WebSocketSession): Mono<Void> {

        logger.info("Handling session {}", session)

        return session.send(webSocketApiImpl.send(productRepository.products().map { p -> p.id })
            .map(session::textMessage))
            .and(session.receive().map { webSocketMessage -> webSocketApiImpl.receive(webSocketMessage.payloadAsText) })
    }

}
