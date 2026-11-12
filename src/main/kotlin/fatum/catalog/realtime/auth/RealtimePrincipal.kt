package fatum.catalog.realtime.auth

import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import java.security.Principal

/**
 * Identidad de la sesión STOMP. `name` es el id de usuario del dominio (el `sub` de Cognito),
 * que es lo que usa `SimpMessagingTemplate.convertAndSendToUser`.
 */
class RealtimePrincipal(val userId: String) : Principal {
    override fun getName(): String = userId
    override fun toString(): String = "RealtimePrincipal($userId)"
}

fun interface PrincipalResolver {
    /** @return el principal, o `null` si la conexión no trae credenciales. */
    fun resolve(headers: StompHeaderAccessor): Principal?
}

class RealtimeAuthenticationException(message: String, cause: Throwable? = null) :
    MessagingException(message, cause)

class RealtimeAccessDeniedException(message: String) : MessagingException(message)
