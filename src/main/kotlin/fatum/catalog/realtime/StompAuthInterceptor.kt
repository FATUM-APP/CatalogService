package fatum.catalog.realtime

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import java.security.Principal

/**
 * Resuelve quién es el usuario del WebSocket.
 *
 * Con seguridad encendida: valida `Authorization: Bearer <token>` en el CONNECT y usa el `sub`
 * (el mismo id que llega en las órdenes). Sin seguridad: acepta `X-User-Id`, solo para desarrollo.
 */
@Component
class StompAuthInterceptor(
    @Value("\${app.security.enabled:true}") private val securityEnabled: Boolean,
    private val jwtDecoders: ObjectProvider<JwtDecoder>
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message
        if (accessor.command != StompCommand.CONNECT) return message

        val userId = if (securityEnabled) fromToken(accessor) else accessor.getFirstNativeHeader("X-User-Id")
        userId?.let { accessor.user = Principal { it } }
        return message
    }

    private fun fromToken(accessor: StompHeaderAccessor): String? {
        val header = accessor.getFirstNativeHeader("Authorization")
            ?: throw MessagingException("Falta la cabecera Authorization en el CONNECT")
        val token = header.removePrefix("Bearer ").trim()
        val decoder = jwtDecoders.getIfAvailable()
            ?: throw MessagingException("La seguridad está activa pero no hay validador de tokens")

        return runCatching { decoder.decode(token).subject }
            .getOrElse { throw MessagingException("Token inválido", it) }
    }
}
