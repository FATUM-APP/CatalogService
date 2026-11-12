package fatum.catalog.realtime.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import fatum.catalog.realtime.config.RealtimeProperties
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Instant

/** Claims mínimos que necesita el canal. */
data class JwtClaims(
    val subject: String?,
    val issuer: String?,
    val expiresAt: Instant?,
    val raw: Map<String, Any>
) {
    fun stringClaim(name: String): String? = raw[name] as? String
}

interface JwtVerifier {
    fun verify(token: String): JwtClaims
}

/**
 * Verifica el access token de Cognito contra el JWKS del User Pool.
 *
 * `RemoteJWKSet` cachea las claves y las refresca ante un `kid` desconocido, así que la
 * verificación no hace una llamada de red por conexión.
 */
class CognitoJwtVerifier(
    private val config: RealtimeProperties.Auth.Jwt
) : JwtVerifier {

    private val log = LoggerFactory.getLogger(javaClass)

    private val jwkSource: JWKSource<SecurityContext> by lazy {
        val uri = config.jwkSetUri?.takeIf { it.isNotBlank() }
            ?: "${config.issuerUri.trimEnd('/')}/.well-known/jwks.json"
        log.info("Realtime JWT verification enabled with JWKS {}", uri)
        RemoteJWKSet<SecurityContext>(URI(uri).toURL())
    }

    private val processor: DefaultJWTProcessor<SecurityContext> by lazy {
        DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)
        }
    }

    override fun verify(token: String): JwtClaims {
        val claims: JWTClaimsSet = try {
            processor.process(token, null)
        } catch (ex: Exception) {
            throw RealtimeAuthenticationException("Invalid access token", ex)
        }
        val raw = claims.claims
        val issuer = raw["iss"] as? String
        val exp = raw["exp"] as? java.util.Date

        if (config.issuerUri.isNotBlank() && issuer != config.issuerUri)
            throw RealtimeAuthenticationException("Token issuer is not allowed")
        if (exp == null || exp.toInstant().isBefore(Instant.now()))
            throw RealtimeAuthenticationException("Token is expired")
        if (config.acceptedAudiences.isNotEmpty()) {
            val audience = claims.audience.orEmpty()
            if (audience.none { it in config.acceptedAudiences })
                throw RealtimeAuthenticationException("Token audience is not allowed")
        }

        return JwtClaims(
            subject = raw["sub"] as? String,
            issuer = issuer,
            expiresAt = exp?.toInstant(),
            raw = raw
        )
    }
}
