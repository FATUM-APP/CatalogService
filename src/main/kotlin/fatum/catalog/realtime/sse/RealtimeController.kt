package fatum.catalog.realtime.sse

import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * Los canales hacia el navegador.
 *
 * <p>El usuario de las órdenes sale del token, traducido al `userId` de la plataforma: las órdenes
 * guardan ese identificador y no el `sub` de Cognito, así que es el que se usa como tema. El
 * calendario es de un proveedor concreto: lo abren él y sus clientes, y por eso el `providerId` va
 * en la URL.</p>
 *
 * <p>El front se conecta con `EventSource`, que no puede mandar cabeceras: por eso el token viaja
 * como `?access_token=`.</p>
 */
@RestController
@RequestMapping("/realtime")
class RealtimeController(
    private val hub: SseHub,
    private val users: PlatformUserResolver
) {

    @GetMapping("/orders", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun orders(authentication: Authentication): SseEmitter =
        hub.subscribe(SseHub.userTopic(users.resolve(authentication)))

    @GetMapping("/calendar", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun calendar(@RequestParam providerId: String): SseEmitter {
        require(providerId.isNotBlank()) { "providerId es obligatorio" }
        return hub.subscribe(SseHub.calendarTopic(providerId.trim()))
    }
}
