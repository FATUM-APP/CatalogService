package fatum.catalog.client.auth


import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.Base64

@Component
class CognitoTokenProvider(
    @Value("\${cognito.token-url}") private val tokenUrl: String,
    @Value("\${cognito.client-id}") private val clientId: String,
    @Value("\${cognito.client-secret}") private val clientSecret: String,
    @Value("\${cognito.scope}") private val scope: String
) {
    private val restClient = RestClient.create()

    @Volatile
    private var cachedToken: TokenHolder? = null

    @Synchronized
    fun getAccessToken(): String {
        cachedToken?.let { if (!it.isExpired()) return it.token }

        val basicAuth = Base64.getEncoder()
            .encodeToString("$clientId:$clientSecret".toByteArray())

        val response = restClient.post()
            .uri(tokenUrl)
            .header("Authorization", "Basic $basicAuth")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("grant_type=client_credentials&scope=$scope")
            .retrieve()
            .body(CognitoTokenResponse::class.java)
            ?: throw IllegalStateException("No se pudo obtener el token de Cognito")

        val holder = TokenHolder(response.accessToken, response.expiresIn)
        cachedToken = holder
        return holder.token
    }
}

data class CognitoTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
    val tokenType: String
)

private class TokenHolder(val token: String, expiresIn: Long) {
    private val expiresAt = Instant.now().plusSeconds(expiresIn - 30) // margen de 30s
    fun isExpired() = Instant.now().isAfter(expiresAt)
}