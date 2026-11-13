package fatum.catalog.dto.response.order

import fatum.catalog.model.vo.QuestionAnswer
import java.time.LocalDateTime

data class ClientOrder(
    val id: String?,
    val providerUsername: String,
    val catalogItemTitle: String,
    val booking: LocalDateTime,
    val negotiatedPrice: String,
    val estimatedTime: Int?,
    val startConfirmation: DualConfirmationResponse,
    val completionConfirmation: DualConfirmationResponse,
    val paymentConfirmation: DualConfirmationResponse,
    val reason: String? = null,
    val status: String,
)
