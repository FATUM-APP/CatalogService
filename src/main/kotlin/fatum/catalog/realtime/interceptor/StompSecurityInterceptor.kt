package fatum.catalog.realtime.interceptor

import fatum.catalog.realtime.auth.PrincipalResolver
import fatum.catalog.realtime.auth.RealtimeAccessDeniedException
import fatum.catalog.realtime.auth.RealtimeAuthenticationException
import fatum.catalog.realtime.config.RealtimeProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import java.util.concurrent.atomic.AtomicInteger

/**
 * Autentica la sesión en CONNECT y autoriza cada SUBSCRIBE.
 *
 * Reglas:
 *  - CONNECT sin credenciales válidas -> se cierra la sesión con un frame ERROR.
 *  - SUBSCRIBE a `/user/queue/...` -> permitido: Spring lo traduce a una cola privada de la sesión.
 *    Se rechaza cualquier `/user/{otroUsuario}/...` (permitiría leer la bandeja de otro).
 *  - SUBSCRIBE a `/topic/...` -> solo los patrones públicos configurados (calendario).
 *  - SEND -> denegado por defecto: este servicio no consume mensajes de clientes.
 */
@Component
@ConditionalOnProperty(
    prefix = "catalog.realtime",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class StompSecurityInterceptor(
    private val principalResolver: PrincipalResolver,
    private val properties: RealtimeProperties
) : ChannelInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)
    private val pathMatcher = AntPathMatcher()
    private val userQueuePattern = Regex("^/user/queue/[A-Za-z0-9_-]+$")

    init {
        if (!properties.auth.enabled) {
            log.warn(
                "Realtime authentication is DISABLED: any client can claim any userId on CONNECT. " +
                    "Enable it with catalog.realtime.auth.enabled=true before exposing personal data."
            )
        }
    }

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message
        when (accessor.command) {
            StompCommand.CONNECT -> authenticate(accessor)
            StompCommand.SUBSCRIBE -> authorizeSubscription(accessor)
            StompCommand.SEND -> authorizeSend(accessor)
            else -> Unit
        }
        return message
    }

    private fun authenticate(accessor: StompHeaderAccessor) {
        val principal = principalResolver.resolve(accessor)
        if (principal == null) {
            if (properties.auth.enabled)
                throw RealtimeAuthenticationException("Missing credentials on CONNECT frame")
            return
        }
        accessor.user = principal
        log.debug("STOMP session authenticated for user {}", principal.name)
    }

    private fun authorizeSubscription(accessor: StompHeaderAccessor) {
        if (!properties.auth.enabled) return
        val user = accessor.user
            ?: throw RealtimeAuthenticationException("Session is not authenticated")
        val destination = accessor.destination?.trim()
            ?: throw RealtimeAccessDeniedException("SUBSCRIBE frame without destination")

        if (!isAllowedSubscription(destination)) {
            log.warn("User {} tried to subscribe to forbidden destination {}", user.name, destination)
            throw RealtimeAccessDeniedException("Destination '$destination' is not available")
        }
        enforceSubscriptionQuota(accessor, user.name)
    }

    private fun authorizeSend(accessor: StompHeaderAccessor) {
        if (!properties.auth.enabled) return
        if (accessor.user == null) throw RealtimeAuthenticationException("Session is not authenticated")
        val destination = accessor.destination?.trim()
            ?: throw RealtimeAccessDeniedException("SEND frame without destination")
        val allowed = properties.auth.allowedSendPatterns.any { pathMatcher.match(it, destination) }
        if (!allowed)
            throw RealtimeAccessDeniedException("Sending to '$destination' is not allowed")
    }

    private fun isAllowedSubscription(destination: String): Boolean {
        if (destination.startsWith("/user/")) return userQueuePattern.matches(destination)
        if (destination.startsWith("/queue/")) return false
        return properties.auth.publicTopicPatterns.any { pathMatcher.match(it, destination) }
    }

    private fun enforceSubscriptionQuota(accessor: StompHeaderAccessor, userId: String) {
        val limit = properties.auth.maxSubscriptionsPerSession
        if (limit <= 0) return
        val attributes = accessor.sessionAttributes ?: return
        val counter = attributes.computeIfAbsent(QUOTA_KEY) { AtomicInteger() } as AtomicInteger
        val current = counter.incrementAndGet()
        if (current > limit) {
            counter.decrementAndGet()
            log.warn("User {} exceeded the subscription limit ({})", userId, limit)
            throw RealtimeAccessDeniedException("Too many subscriptions on this session (max $limit)")
        }
    }

    private companion object {
        const val QUOTA_KEY = "fatum.realtime.subscriptions"
    }
}
