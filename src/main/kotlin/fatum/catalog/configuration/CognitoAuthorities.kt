package fatum.catalog.configuration

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

/**
 * Los grupos de Cognito llegan en el claim `cognito:groups`.
 * Aquí se convierten en roles de Spring: `PROFESSIONAL` -> `ROLE_PROFESSIONAL`.
 */
object CognitoAuthorities {

    const val GROUPS_CLAIM = "cognito:groups"
    private val scopeConverter = JwtGrantedAuthoritiesConverter()  // default: scope -> SCOPE_x

    fun from(jwt: Jwt): Collection<GrantedAuthority> {
        val groupRoles = (jwt.getClaimAsStringList(GROUPS_CLAIM) ?: emptyList())
            .map { SimpleGrantedAuthority("ROLE_$it") }
        val scopeAuthorities = scopeConverter.convert(jwt) ?: emptyList()
        return groupRoles + scopeAuthorities
    }
}
