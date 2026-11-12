package fatum.catalog.event

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class SseEmitterRegistry {
    private val userEmitters = ConcurrentHashMap<String, SseEmitter>()
    private val topicSubscribers = ConcurrentHashMap<String, MutableList<SseEmitter>>()

    fun register(userId: String): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)
        userEmitters[userId] = emitter
        emitter.onCompletion { userEmitters.remove(userId) }
        emitter.onTimeout { userEmitters.remove(userId) }
        return emitter
    }

    fun subscribe(topic: String, emitter: SseEmitter) {
        topicSubscribers.computeIfAbsent(topic) { mutableListOf() }.add(emitter)
        emitter.onCompletion { topicSubscribers[topic]?.remove(emitter) }
        emitter.onTimeout { topicSubscribers[topic]?.remove(emitter) }
    }

    fun sendToUser(userId: String, eventName: String, payload: Any) {
        userEmitters[userId]?.send(
            SseEmitter.event().name(eventName).data(payload)
        )
    }

    fun sendToTopic(topic: String, eventName: String, payload: Any) {
        topicSubscribers[topic]?.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload))
            } catch (e: Exception) {
                topicSubscribers[topic]?.remove(emitter)
            }
        }
    }
}