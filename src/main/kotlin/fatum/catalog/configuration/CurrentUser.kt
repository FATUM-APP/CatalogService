package fatum.catalog.configuration

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Quién está pidiendo, según el token.
 *
 * <p>Se lee el `sub` de Cognito y no el `name` del `Authentication`: el `name` depende de cómo se
 * haya armado el token, mientras que el `sub` es el identificador que usa toda la plataforma.</p>
 */
object CurrentUser {

    fun id(authentication: Authentication): String =
        (authentication as? JwtAuthenticationToken)?.token?.subject ?: authentication.name
}
