package fatum.catalog.realtime.sse

import fatum.catalog.realtime.RealtimeProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

/**
 * Las conexiones abiertas, agrupadas por tema.
 *
 * <p>Hay dos clases de tema:</p>
 * <ul>
 *   <li>`user:<id>` — las órdenes de esa persona. Nadie más las ve.</li>
 *   <li>`calendar:<providerId>` — el calendario de un proveedor. Lo abre el propio proveedor y
 *   también cualquier cliente que esté mirando sus horarios, así que es un tema compartido.</li>
 * </ul>
 *
 * <p>Vive en memoria, en la instancia que atiende la conexión. Con una sola tarea por servicio
 * alcanza; si algún día hay varias, el evento publicado en una no llega a los clientes de la otra y
 * habría que pasar por un bus compartido (Redis, por ejemplo).</p>
 */
@Component
class SseHub(private val properties: RealtimeProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val connections = ConcurrentHashMap<String, MutableSet<SseEmitter>>()

    fun subscribe(topic: String): SseEmitter {
        val emitter = SseEmitter(properties.timeoutMs)
        val topicConnections = connections.computeIfAbsent(topic) { ConcurrentHashMap.newKeySet() }

        if (topicConnections.size >= properties.maxConnectionsPerUser) {
            // Se cierra la más vieja en vez de rechazar la nueva: suele ser una pestaña olvidada.
            topicConnections.firstOrNull()?.let { remove(topic, it) }
        }

        topicConnections.add(emitter)
        emitter.onCompletion { remove(topic, emitter) }
        emitter.onTimeout { remove(topic, emitter) }
        emitter.onError { remove(topic, emitter) }

        send(emitter, SseEmitter.event().name("connected").data(mapOf("status" to "connected")))
        return emitter
    }

    /**
     * Le escribe a cada tema de la lista. `name` es el nombre del evento SSE (`order`, `calendar`),
     * que es lo que el front escucha con `addEventListener`.
     */
    fun publish(topics: Collection<String>, name: String, event: Map<String, Any?>) {
        val targets = topics.filter { it.isNotBlank() }.distinct()
        if (targets.isEmpty()) return

        targets.forEach { topic ->
            connections[topic]?.toList()?.forEach { emitter ->
                try {
                    send(emitter, SseEmitter.event().name(name).data(event))
                } catch (e: Exception) {
                    log.debug("Se cierra una conexión de {}: {}", topic, e.message)
                    remove(topic, emitter)
                }
            }
        }
    }

    /**
     * Latido. Sin esto, el ALB corta la conexión al minuto de silencio y el navegador reconecta una
     * y otra vez.
     */
    @Scheduled(fixedRateString = "\${fatum.realtime.heartbeat-ms:20000}")
    fun heartbeat() {
        connections.forEach { (topic, topicConnections) ->
            topicConnections.toList().forEach { emitter ->
                try {
                    send(emitter, SseEmitter.event().comment("ping"))
                } catch (e: Exception) {
                    remove(topic, emitter)
                }
            }
        }
    }

    internal fun connectionCount(topic: String): Int = connections[topic]?.size ?: 0

    /** Un emisor no admite dos envíos a la vez. */
    private fun send(emitter: SseEmitter, event: SseEmitter.SseEventBuilder) {
        synchronized(emitter) {
            emitter.send(event)
        }
    }

    private fun remove(topic: String, emitter: SseEmitter) {
        connections[topic]?.remove(emitter)
        runCatching { emitter.complete() }
    }

    companion object {
        fun userTopic(userId: String): String = "user:$userId"

        fun calendarTopic(providerId: String): String = "calendar:$providerId"
    }
}
