package fatum.catalog.realtime

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * STOMP sobre WebSocket, igual que la guía de Spring.
 *
 *   wss://api.tu-dominio/ws/catalog      handshake (los orígenes salen de app.cors.allowed-origins)
 *   /topic/calendar/{providerId}         calendario público de un proveedor
 *   /user/queue/orders                   órdenes del usuario conectado
 *
 * El CONNECT se valida en [StompAuthInterceptor].
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    @Value("\${app.cors.allowed-origins:http://localhost:5173}") private val allowedOrigins: String,
    private val stompAuthInterceptor: StompAuthInterceptor
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic", "/queue")
        config.setApplicationDestinationPrefixes("/app")
        config.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        registry.addEndpoint("/ws/catalog").setAllowedOriginPatterns(*origins.toTypedArray())
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompAuthInterceptor)
    }
}
