package fatum.catalog.realtime

import fatum.catalog.realtime.auth.AnonymousPrincipalResolver
import fatum.catalog.realtime.auth.JwtPrincipalResolver
import fatum.catalog.realtime.auth.PrincipalResolver
import fatum.catalog.realtime.config.RealtimeInfrastructureConfig
import fatum.catalog.realtime.config.RealtimeProperties
import fatum.catalog.realtime.config.WebSocketConfig
import fatum.catalog.realtime.interceptor.StompSecurityInterceptor
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.messaging.simp.SimpMessagingTemplate

/**
 * Verifica el cableado del canal sin levantar base de datos: endpoint, broker, scheduler de
 * heartbeats y resolución de identidad.
 */
class RealtimeContextTest {

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java)

    @Configuration
    @EnableConfigurationProperties(RealtimeProperties::class)
    @Import(RealtimeInfrastructureConfig::class, WebSocketConfig::class, StompSecurityInterceptor::class)
    class TestConfig

    @Test
    fun `should wire the stomp infrastructure without authentication by default`() {
        runner.run { context ->
            assertNotNull(context.getBean(SimpMessagingTemplate::class.java))
            assertInstanceOf(AnonymousPrincipalResolver::class.java, context.getBean(PrincipalResolver::class.java))
            assertNotNull(context.getBean(StompSecurityInterceptor::class.java))
            assertNotNull(context.getBean("realtimeHeartbeatScheduler"))
        }
    }

    @Test
    fun `should use jwt verification when authentication is enabled`() {
        runner.withPropertyValues(
            "catalog.realtime.auth.enabled=true",
            "catalog.realtime.auth.jwt.issuer-uri=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_abc"
        ).run { context ->
            assertInstanceOf(JwtPrincipalResolver::class.java, context.getBean(PrincipalResolver::class.java))
        }
    }

    @Test
    fun `should not start the channel when it is disabled`() {
        runner.withPropertyValues("catalog.realtime.enabled=false").run { context ->
            assertFalse(context.containsBean("webSocketConfig"))
        }
    }
}
