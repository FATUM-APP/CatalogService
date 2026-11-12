package fatum.catalog.realtime

import fatum.catalog.realtime.auth.PrincipalResolver
import fatum.catalog.realtime.auth.RealtimeAccessDeniedException
import fatum.catalog.realtime.auth.RealtimeAuthenticationException
import fatum.catalog.realtime.auth.RealtimePrincipal
import fatum.catalog.realtime.config.RealtimeProperties
import fatum.catalog.realtime.interceptor.StompSecurityInterceptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.support.MessageHeaderAccessor

class StompSecurityInterceptorTest {

    private val channel: MessageChannel = mock()

    private val authEnabled = RealtimeProperties(auth = RealtimeProperties.Auth(enabled = true))

    private fun interceptor(
        properties: RealtimeProperties = authEnabled,
        resolver: PrincipalResolver = PrincipalResolver { headers ->
            headers.getFirstNativeHeader("Authorization")
                ?.removePrefix("Bearer ")
                ?.let { RealtimePrincipal("user-1") }
        }
    ) = StompSecurityInterceptor(resolver, properties)

    private fun frame(
        command: StompCommand,
        destination: String? = null,
        headers: Map<String, String> = emptyMap(),
        user: RealtimePrincipal? = null,
        attributes: MutableMap<String, Any> = mutableMapOf()
    ): Message<*> {
        val accessor = StompHeaderAccessor.create(command)
        destination?.let { accessor.destination = it }
        headers.forEach { (name, value) -> accessor.addNativeHeader(name, value) }
        accessor.user = user
        accessor.sessionAttributes = attributes
        accessor.setLeaveMutable(true)
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    @Test
    fun `should authenticate a connect frame with a bearer token`() {
        val message = interceptor().preSend(frame(StompCommand.CONNECT, headers = mapOf("Authorization" to "Bearer abc")), channel)

        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        assertNotNull(accessor)
        assertEquals("user-1", accessor!!.user?.name)
    }

    @Test
    fun `should reject a connect frame without credentials`() {
        assertThrows(RealtimeAuthenticationException::class.java) {
            interceptor().preSend(frame(StompCommand.CONNECT), channel)
        }
    }

    @Test
    fun `should reject a subscribe frame on a session without principal`() {
        assertThrows(RealtimeAuthenticationException::class.java) {
            interceptor().preSend(frame(StompCommand.SUBSCRIBE, destination = "/user/queue/orders"), channel)
        }
    }

    @Test
    fun `should allow the private order queue of the authenticated user`() {
        interceptor().preSend(
            frame(StompCommand.SUBSCRIBE, destination = "/user/queue/orders", user = RealtimePrincipal("user-1")),
            channel
        )
    }

    @Test
    fun `should allow subscribing to the public calendar of a provider`() {
        interceptor().preSend(
            frame(StompCommand.SUBSCRIBE, destination = "/topic/calendar/provider-1", user = RealtimePrincipal("user-1")),
            channel
        )
    }

    @Test
    fun `should not allow reading another user queue`() {
        assertThrows(RealtimeAccessDeniedException::class.java) {
            interceptor().preSend(
                frame(StompCommand.SUBSCRIBE, destination = "/user/victim/queue/orders", user = RealtimePrincipal("user-1")),
                channel
            )
        }
    }

    @Test
    fun `should not allow unknown topics`() {
        assertThrows(RealtimeAccessDeniedException::class.java) {
            interceptor().preSend(
                frame(StompCommand.SUBSCRIBE, destination = "/topic/orders", user = RealtimePrincipal("user-1")),
                channel
            )
        }
    }

    @Test
    fun `should not allow subscribing directly to a broker queue`() {
        assertThrows(RealtimeAccessDeniedException::class.java) {
            interceptor().preSend(
                frame(StompCommand.SUBSCRIBE, destination = "/queue/orders", user = RealtimePrincipal("user-1")),
                channel
            )
        }
    }

    @Test
    fun `should not allow sending messages from the client`() {
        assertThrows(RealtimeAccessDeniedException::class.java) {
            interceptor().preSend(
                frame(StompCommand.SEND, destination = "/app/orders", user = RealtimePrincipal("user-1")),
                channel
            )
        }
    }

    @Test
    fun `should enforce the subscription quota per session`() {
        val properties = RealtimeProperties(
            auth = RealtimeProperties.Auth(enabled = true, maxSubscriptionsPerSession = 1)
        )
        val attributes = mutableMapOf<String, Any>()
        val security = interceptor(properties)
        security.preSend(
            frame(StompCommand.SUBSCRIBE, destination = "/user/queue/orders", user = RealtimePrincipal("user-1"), attributes = attributes),
            channel
        )
        assertThrows(RealtimeAccessDeniedException::class.java) {
            security.preSend(
                frame(StompCommand.SUBSCRIBE, destination = "/topic/calendar/p1", user = RealtimePrincipal("user-1"), attributes = attributes),
                channel
            )
        }
    }

    @Test
    fun `should skip authorization when realtime auth is disabled`() {
        val properties = RealtimeProperties(auth = RealtimeProperties.Auth(enabled = false))
        interceptor(properties).preSend(frame(StompCommand.SUBSCRIBE, destination = "/topic/anything"), channel)
    }
}
