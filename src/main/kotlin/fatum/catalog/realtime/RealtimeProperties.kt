package fatum.catalog.realtime

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Ajustes del canal en tiempo real.
 *
 * <p>`timeout` es cuánto vive una conexión abierta: cuando se cierra, el navegador de `EventSource`
 * vuelve a conectarse solo. `heartbeat-ms` es el latido que impide que el ALB corte la conexión por
 * estar inactiva (su timeout por defecto es de 60 segundos).</p>
 */
@ConfigurationProperties(prefix = "fatum.realtime")
data class RealtimeProperties(
    val enabled: Boolean = true,
    val timeoutMs: Long = 30 * 60 * 1000,
    val heartbeatMs: Long = 20 * 1000,
    val maxConnectionsPerUser: Int = 5
)
