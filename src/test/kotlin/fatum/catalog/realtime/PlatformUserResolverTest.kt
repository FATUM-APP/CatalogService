package fatum.catalog.realtime

import fatum.catalog.realtime.sse.PlatformUserResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class PlatformUserResolverTest {

    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val resolver = PlatformUserResolver("https://user.test", builder)

    private fun token(sub: String) = JwtAuthenticationToken(
        Jwt.withTokenValue("the-token").header("alg", "none").claim("sub", sub).build()
    )

    @Test
    fun `translates the token subject to the platform user id`() {
        server.expect(requestTo("https://user.test/users/me"))
            .andExpect(header("Authorization", "Bearer the-token"))
            .andRespond(withSuccess("""{"userId":"user-uuid-1","name":"Ana"}""", MediaType.APPLICATION_JSON))

        assertEquals("user-uuid-1", resolver.resolve(token("cognito-sub-1")))
        server.verify()
    }

    @Test
    fun `asks once and remembers the answer`() {
        server.expect(requestTo("https://user.test/users/me"))
            .andRespond(withSuccess("""{"userId":"user-uuid-1"}""", MediaType.APPLICATION_JSON))

        resolver.resolve(token("cognito-sub-1"))
        resolver.resolve(token("cognito-sub-1"))

        // Una sola petición: la segunda sale de la caché.
        server.verify()
    }

    @Test
    fun `falls back to the subject when the user service fails`() {
        server.expect(requestTo("https://user.test/users/me"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertEquals("cognito-sub-1", resolver.resolve(token("cognito-sub-1")))
    }
}
