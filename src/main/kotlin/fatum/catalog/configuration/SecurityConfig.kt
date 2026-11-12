package fatum.catalog.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

/**
 * Validación del token de Cognito y reglas de acceso.
 *
 *   ADMIN         las categorías del catálogo
 *   PROFESSIONAL  métricas, catálogo (crear/editar/activar), disponibilidad e indisponibilidad
 *   VERIFIED      el resto: lecturas, solicitar órdenes y favoritos
 *
 * Un ADMIN y un PROFESSIONAL también pasan las reglas de VERIFIED.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = ["app.security.enabled"], havingValue = "true", matchIfMissing = true)
class SecurityConfig(
    @Value("\${app.security.issuer-uri}") private val issuerUri: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .cors { }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { auth ->
            auth.requestMatchers("/actuator/health/**").permitAll()

            // --- ADMIN ---
            auth.requestMatchers(HttpMethod.POST, "/catalog/category/**").hasRole(ADMIN)
            auth.requestMatchers(HttpMethod.PUT, "/catalog/category/**").hasRole(ADMIN)

            // --- PROFESSIONAL ---
            auth.requestMatchers("/metrics/**").hasRole(PROFESSIONAL)
            auth.requestMatchers(HttpMethod.POST, "/catalog/item/create").hasRole(PROFESSIONAL)
            auth.requestMatchers(
                HttpMethod.PUT,
                "/catalog/item/update",
                "/catalog/item/deactivate",
                "/catalog/item/activate"
            ).hasRole(PROFESSIONAL)
            auth.requestMatchers(HttpMethod.PUT, "/provider/availability/**").hasRole(PROFESSIONAL)
            auth.requestMatchers(HttpMethod.DELETE, "/provider/availability/**").hasRole(PROFESSIONAL)
            auth.requestMatchers(HttpMethod.POST, "/provider/unavailability/**").hasRole(PROFESSIONAL)
            auth.requestMatchers(HttpMethod.PUT, "/provider/unavailability/**").hasRole(PROFESSIONAL)
            auth.requestMatchers(HttpMethod.DELETE, "/provider/unavailability/**").hasRole(PROFESSIONAL)

            // --- VERIFIED (o PROFESSIONAL, o ADMIN) ---
            auth.anyRequest().hasAnyRole(VERIFIED, PROFESSIONAL, ADMIN)
        }
        .oauth2ResourceServer { oauth2 ->
            oauth2.bearerTokenResolver(bearerTokenResolver())
            oauth2.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
        }
        .build()

    /**
     * Cognito publica las llaves en `{issuer}/.well-known/jwks.json`.
     * Se arma la URL a mano para no depender del documento de descubrimiento al arrancar.
     */
    /**
     * `EventSource` no puede mandar cabeceras, así que además de `Authorization` se acepta
     * `?access_token=`.
     */
    @Bean
    fun bearerTokenResolver(): DefaultBearerTokenResolver =
        DefaultBearerTokenResolver().apply { setAllowUriQueryParameter(true) }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val jwkSetUri = issuerUri.trimEnd('/') + "/.well-known/jwks.json"
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build().apply {
            setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri))
        }
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter { jwt -> CognitoAuthorities.from(jwt) }
        }

    companion object {
        const val ADMIN = "ADMIN"
        const val PROFESSIONAL = "PROFESSIONAL"
        const val VERIFIED = "VERIFIED"
    }
}
