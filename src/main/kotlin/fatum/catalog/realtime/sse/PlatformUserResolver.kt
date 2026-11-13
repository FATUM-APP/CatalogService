package fatum.catalog.realtime.sse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Traduce el `sub` del token al identificador que usa el resto de la plataforma.
 *
 * <p>Las órdenes guardan el `userId` del UserService (el UUID de la columna `ID`), mientras que el
 * token de Cognito trae el `sub` (la columna `AWS_ID`). No son el mismo valor: si el canal se
 * abriera con el `sub`, el evento se publicaría en `user:<userId>` y la conexión viviría en
 * `user:<sub>`, así que nunca llegaría nada.</p>
 *
 * <p>Se pide una vez por conexión y se guarda unos minutos: `EventSource` reconecta cada media
 * hora y no tiene sentido consultar al UserService en cada reconexión.</p>
 */
@Component
class PlatformUserResolver(
    @Value("\${fatum.base-url}") baseUrl: String,
    builder: RestClient.Builder = RestClient.builder()
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = builder.baseUrl(baseUrl).build()

    private val cache = ConcurrentHashMap<String, Cached>()

    fun resolve(authentication: Authentication): String {
        val jwt = (authentication as? JwtAuthenticationToken)?.token
            ?: return authentication.name

        val sub = jwt.subject ?: authentication.name
        cache[sub]?.takeIf { it.expiresAt.isAfter(Instant.now()) }?.let { return it.userId }

        val userId = runCatching {
            restClient.get()
                .uri("/users/me")
                .header("Authorization", "Bearer ${jwt.tokenValue}")
                .retrieve()
                .body(PlatformUser::class.java)
                ?.userId
        }.getOrElse { error ->
            log.warn("No se pudo resolver el usuario {} en el UserService: {}", sub, error.message)
            null
        }

        if (userId.isNullOrBlank()) {
            // Con el `sub` el canal abre pero no recibe nada; queda en el log para no fallar en silencio.
            log.warn("El canal de {} se abre con el sub del token: no llegarán eventos de órdenes", sub)
            return sub
        }

        cache[sub] = Cached(userId, Instant.now().plus(CACHE_TTL))
        return userId
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PlatformUser(val userId: String)

    private data class Cached(val userId: String, val expiresAt: Instant)

    private companion object {
        val CACHE_TTL: Duration = Duration.ofMinutes(5)
    }
}
