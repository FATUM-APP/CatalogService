package fatum.catalog.realtime.config

import fatum.catalog.realtime.RealtimeTopics
import fatum.catalog.realtime.interceptor.StompSecurityInterceptor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration

/**
 * STOMP sobre WebSocket.
 *
 *   endpoint   : /ws/catalog            (nativo)  |  /ws/catalog-sockjs (fallback SockJS)
 *   app prefix : /app                   (mensajes entrantes, deshabilitados por defecto)
 *   broker     : /topic  (público)  /queue (colas privadas por usuario)
 *
 * El broker puede ser simple (en memoria, una sola instancia) o un relay STOMP a RabbitMQ /
 * Artemis cuando el servicio escala horizontalmente.
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(
    prefix = "catalog.realtime",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class WebSocketConfig(
    private val properties: RealtimeProperties,
    private val securityInterceptor: StompSecurityInterceptor,
    @Qualifier("realtimeHeartbeatScheduler") private val heartbeatScheduler: ThreadPoolTaskScheduler
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        val prefixes = properties.broker.prefixes.toTypedArray()

        when (properties.broker.mode) {
            RealtimeProperties.Broker.Mode.SIMPLE ->
                registry.enableSimpleBroker(*prefixes)
                    .setHeartbeatValue(
                        longArrayOf(properties.heartbeat.interval, properties.heartbeat.interval)
                    )
                    .setTaskScheduler(heartbeatScheduler)

            RealtimeProperties.Broker.Mode.RELAY ->
                registry.enableStompBrokerRelay(*prefixes).apply {
                    val relay = properties.broker.relay
                    setRelayHost(relay.host)
                    setRelayPort(relay.port)
                    setClientLogin(relay.clientLogin)
                    setClientPasscode(relay.clientPasscode)
                    setSystemLogin(relay.systemLogin)
                    setSystemPasscode(relay.systemPasscode)
                    setSystemHeartbeatSendInterval(relay.systemHeartbeatSendInterval)
                    setSystemHeartbeatReceiveInterval(relay.systemHeartbeatReceiveInterval)
                    relay.virtualHost?.let { setVirtualHost(it) }
                }
        }

        registry.setApplicationDestinationPrefixes(RealtimeTopics.APP_PREFIX)
        registry.setUserDestinationPrefix(RealtimeTopics.USER_PREFIX)
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = properties.allowedOrigins.toTypedArray()
        registry.addEndpoint(properties.endpoint)
            .setAllowedOriginPatterns(*origins)

        properties.sockJsEndpoint?.takeIf { it.isNotBlank() }?.let { path ->
            registry.addEndpoint(path)
                .setAllowedOriginPatterns(*origins)
                .withSockJS()
        }
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(securityInterceptor)
    }

    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration
            .setMessageSizeLimit(properties.limits.messageSize)
            .setSendBufferSizeLimit(properties.limits.sendBufferSize)
            .setSendTimeLimit(properties.limits.sendTimeLimit.toInt())
    }
}
