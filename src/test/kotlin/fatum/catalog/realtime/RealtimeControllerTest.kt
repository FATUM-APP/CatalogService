package fatum.catalog.realtime

import fatum.catalog.realtime.sse.RealtimeController
import fatum.catalog.realtime.sse.SseHub
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class RealtimeControllerTest {

    private val hub: SseHub = mock()
    private val controller = RealtimeController(hub)

    private fun token(sub: String) = JwtAuthenticationToken(
        Jwt.withTokenValue("token").header("alg", "none").claim("sub", sub).build()
    )

    @Test
    fun `the order stream belongs to whoever owns the token`() {
        val emitter = SseEmitter()
        whenever(hub.subscribe(SseHub.userTopic("user-1"))).thenReturn(emitter)

        assertEquals(emitter, controller.orders(token("user-1")))
        verify(hub).subscribe(SseHub.userTopic("user-1"))
    }

    @Test
    fun `the calendar stream belongs to the provider of the url`() {
        val emitter = SseEmitter()
        whenever(hub.subscribe(SseHub.calendarTopic("provider-1"))).thenReturn(emitter)

        assertEquals(emitter, controller.calendar("provider-1"))
        verify(hub).subscribe(SseHub.calendarTopic("provider-1"))
    }

    @Test
    fun `a calendar without provider is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { controller.calendar("  ") }
    }
}
