package allanlewis.spring

import allanlewis.api.WebSocketApiImpl
import allanlewis.products.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.StandardWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import javax.websocket.ContainerProvider

class DefaultWebSocketClient(private val webSocketUrl: String, private val webSocketHandler: DefaultWebSocketHandler) {

    private val size = 2048000
    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var webSocketClient: StandardWebSocketClient

    fun init(): DefaultWebSocketClient {
        val container = ContainerProvider.getWebSocketContainer()
        container.defaultMaxTextMessageBufferSize = size
        container.defaultMaxBinaryMessageBufferSize = size

        webSocketClient = StandardWebSocketClient(container)

        webSocketHandler.closed().subscribe { cs ->
            logger.info("Detected that socket was closed, re-connecting {}", cs)

            connect()
        }

        connect()

        return this
    }

    private fun connect() {
        val uri = URI.create(webSocketUrl)

        logger.info("Connecting a websocket to {}", uri)

        webSocketClient.execute(uri, webSocketHandler).subscribe()
    }

}

class DefaultWebSocketHandler(private val webSocketApiImpl: WebSocketApiImpl,
                              private val productRepository: ProductRepository): WebSocketHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var sink: FluxSink<CloseStatus>
    private var session: WebSocketSession? = null
    private var lastMessageReceived: LocalDateTime? = null

    private val flux: Flux<CloseStatus> = Flux.create {
        sink -> this.sink = sink
    }

    private val checker = Flux.interval(Duration.ofSeconds(10))

    override fun handle(session: WebSocketSession): Mono<Void> {
        this.session = session

        logger.info("Handling session {}", session)

        return session.send(webSocketApiImpl.send(productRepository.products().map { p -> p.id })
            .map(session::textMessage))
            .and(session.receive().map { webSocketMessage ->
                lastMessageReceived = LocalDateTime.now()

                webSocketApiImpl.receive(webSocketMessage.payloadAsText)
            })
            .and(session.closeStatus().map { cs ->
                logger.info("Session closed: {}", cs)

                sink.next(cs)
            })
    }

    fun closed(): Flux<CloseStatus> {
        return flux
    }

    fun init(): DefaultWebSocketHandler {
        checker.subscribe {
            if (session != null) {
                logger.info("[check_state] {} {} {}", session.hashCode(), session!!.isOpen, lastMessageReceived)
                if (!session!!.isOpen) {
                    logger.error("Session is not open, forcing a close")

                    sink.next(CloseStatus.SERVER_ERROR)
                } else if (Duration.between(lastMessageReceived, LocalDateTime.now()).seconds > 60) {
                    logger.error("No messages recieved, forcing a close")

                    session!!.close(CloseStatus.BAD_DATA).subscribe { logger.info("Closed session") }
                }
            } else {
                logger.warn("[check_state] Session is still null")
            }
        }

        return this
    }

}
