package fatum.catalog.realtime.config

import fatum.catalog.realtime.auth.AnonymousPrincipalResolver
import fatum.catalog.realtime.auth.CognitoJwtVerifier
import fatum.catalog.realtime.auth.GatewayHeaderPrincipalResolver
import fatum.catalog.realtime.auth.JwtPrincipalResolver
import fatum.catalog.realtime.auth.PrincipalResolver
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * Beans de infraestructura del canal en tiempo real, separados de [WebSocketConfig] para evitar
 * dependencias circulares entre la configuración y sus propios beans.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "catalog.realtime",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class RealtimeInfrastructureConfig {

    /** Scheduler dedicado a los heartbeats del broker simple (no compite con el pool de negocio). */
    @Bean
    fun realtimeHeartbeatScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("realtime-heartbeat-")
            setDaemon(true)
        }

    @Bean
    fun realtimePrincipalResolver(properties: RealtimeProperties): PrincipalResolver {
        if (!properties.auth.enabled) return AnonymousPrincipalResolver()
        return when (properties.auth.mode) {
            RealtimeProperties.Auth.Mode.JWT ->
                JwtPrincipalResolver(
                    CognitoJwtVerifier(properties.auth.jwt),
                    properties.auth.jwt.principalClaim
                )

            RealtimeProperties.Auth.Mode.GATEWAY_HEADER ->
                GatewayHeaderPrincipalResolver(properties.auth.gatewayHeader.name)
        }
    }
}
