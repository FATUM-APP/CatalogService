package fatum.catalog.realtime.auth

import org.springframework.http.HttpHeaders
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import java.security.Principal

/**
 * Resuelve la identidad a partir del access token enviado en el frame CONNECT:
 *
 *    CONNECT
 *    Authorization:Bearer <jwt>
 *
 * (también acepta `access_token:<jwt>` para clientes SockJS que no pueden fijar `Authorization`).
 */
class JwtPrincipalResolver(
    private val verifier: JwtVerifier,
    private val principalClaim: String = "sub"
) : PrincipalResolver {

    override fun resolve(headers: StompHeaderAccessor): Principal? {
        val token = bearerToken(headers) ?: return null
        val claims = verifier.verify(token)
        val userId = claims.stringClaim(principalClaim)?.trim()
            ?: throw RealtimeAuthenticationException("Token does not contain claim '$principalClaim'")
        return RealtimePrincipal(userId)
    }

    private fun bearerToken(headers: StompHeaderAccessor): String? {
        val authorization = headers.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)
        if (!authorization.isNullOrBlank()) {
            if (!authorization.startsWith("Bearer ", ignoreCase = true))
                throw RealtimeAuthenticationException("Authorization header must use the Bearer scheme")
            return authorization.substring(7).trim().ifBlank { null }
        }
        return headers.getFirstNativeHeader("access_token")?.trim()?.ifBlank { null }
    }
}

/**
 * Modo para despliegues donde el borde de confianza (ALB + autorizador / API Gateway) ya validó
 * el token y propaga el id del usuario en una cabecera. Solo debe usarse si el servicio NO es
 * alcanzable directamente desde Internet.
 */
class GatewayHeaderPrincipalResolver(
    private val headerName: String = "X-User-Id"
) : PrincipalResolver {

    override fun resolve(headers: StompHeaderAccessor): Principal? =
        headers.getFirstNativeHeader(headerName)?.trim()?.ifBlank { null }
            ?.let { RealtimePrincipal(it) }
}

/** Usado cuando `catalog.realtime.auth.enabled=false` (desarrollo local). */
class AnonymousPrincipalResolver : PrincipalResolver {
    override fun resolve(headers: StompHeaderAccessor): Principal? =
        headers.getFirstNativeHeader("X-User-Id")?.trim()?.ifBlank { null }
            ?.let { RealtimePrincipal(it) }
}
