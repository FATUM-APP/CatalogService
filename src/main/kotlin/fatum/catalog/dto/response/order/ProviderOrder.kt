package fatum.catalog.dto.response.order

import fatum.catalog.model.vo.QuestionAnswer
import java.time.LocalDateTime

data class ProviderOrder(
    val id: String?,
    val clientUsername: String,
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
