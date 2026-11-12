package fatum.catalog.realtime.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuración del canal de tiempo real (STOMP sobre WebSocket).
 *
 * Todo lo que puede cambiar entre entornos (orígenes permitidos, broker, verificación de tokens,
 * tamaños de payload y ventanas de parcheo del calendario) vive aquí, de forma que el código de
 * dominio no conozca detalles de infraestructura.
 */
@ConfigurationProperties(prefix = "catalog.realtime")
data class RealtimeProperties(
    val enabled: Boolean = true,
    val endpoint: String = "/ws/catalog",
    val sockJsEndpoint: String? = null,
    val allowedOrigins: List<String> = listOf("*"),
    val heartbeat: Heartbeat = Heartbeat(),
    val limits: Limits = Limits(),
    val broker: Broker = Broker(),
    val auth: Auth = Auth(),
    val calendar: CalendarSettings = CalendarSettings()
) {

    data class Heartbeat(
        val interval: Long = 25_000
    )

    data class Limits(
        val messageSize: Int = 64 * 1024,
        val sendBufferSize: Int = 512 * 1024,
        val sendTimeLimit: Long = 10_000
    )

    data class Broker(
        val mode: Mode = Mode.SIMPLE,
        val prefixes: List<String> = listOf("/topic", "/queue"),
        val relay: Relay = Relay()
    ) {
        enum class Mode { SIMPLE, RELAY }

        /**
         * Relay STOMP (RabbitMQ / ActiveMQ Artemis). Necesario cuando el servicio corre con más de
         * una instancia: el broker simple es en memoria y un mensaje publicado en la instancia A no
         * llega a un cliente conectado a la instancia B.
         */
        data class Relay(
            val host: String = "localhost",
            val port: Int = 61613,
            val clientLogin: String = "guest",
            val clientPasscode: String = "guest",
            val systemLogin: String = "guest",
            val systemPasscode: String = "guest",
            val virtualHost: String? = null,
            val systemHeartbeatSendInterval: Long = 10_000,
            val systemHeartbeatReceiveInterval: Long = 10_000
        )
    }

    data class Auth(
        /**
         * Apagada por defecto: el resto del servicio hoy confía en el borde (los endpoints REST
         * reciben `clientId` sin validar token). Al activarla, el `CONNECT` exige un token válido.
         * Con auth apagada el cliente debe enviar `X-User-Id` en el CONNECT para recibir su cola.
         */
        val enabled: Boolean = false,
        val mode: Mode = Mode.JWT,
        /** Destinos de solo lectura abiertos a cualquier usuario autenticado. */
        val publicTopicPatterns: List<String> = listOf("/topic/calendar/**"),
        /** Destinos a los que el cliente puede enviar mensajes. Vacío = el canal es solo de lectura. */
        val allowedSendPatterns: List<String> = emptyList(),
        val maxSubscriptionsPerSession: Int = 30,
        val jwt: Jwt = Jwt(),
        val gatewayHeader: GatewayHeader = GatewayHeader()
    ) {
        enum class Mode { JWT, GATEWAY_HEADER }

        data class Jwt(
            val issuerUri: String = "",
            val jwkSetUri: String? = null,
            /** Claim del token que se usa como id de usuario. En Cognito el `sub` es el userId. */
            val principalClaim: String = "sub",
            val acceptedAudiences: List<String> = emptyList()
        )

        /** Modo para despliegues donde un borde de confianza (ALB + autorizador) ya validó el token. */
        data class GatewayHeader(
            val name: String = "X-User-Id"
        )
    }

    data class CalendarSettings(
        /** Máximo de días que se recalculan y envían en un solo parche de calendario. */
        val maxPatchDays: Int = 62
    )
}
