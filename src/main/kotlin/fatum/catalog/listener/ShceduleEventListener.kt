package fatum.catalog.listener

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class ShceduleEventListener(
    private val messagingTemplate: SimpMessagingTemplate
) {



}