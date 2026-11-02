package fatum.catalog.dto

import fatum.catalog.model.constant.OrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class OrderResponse(
    @field:NotBlank(message = "Order id is required")
    @field:Size(max = 36, message = "Order id must not exceed 36 characters")
    val _orderId: String,

    @field:NotBlank(message = "Provider id is required")
    @field:Size(max = 36, message = "Provider id must not exceed 36 characters")
    val _providerId: String,

    @field:NotNull(message = "Order status is required")
    val status: OrderStatus,

    @field:PositiveOrZero(message = "Estimated time cannot be negative")
    val estimatedTime: Int? = null,

    @field:Size(max=500, message = "Reason must not exceed 500 characters")
    val _reason: String? = null
){
    val orderId: String = _orderId.trim()
    val providerId: String = _providerId.trim()
    val reason: String? = _reason?.trim()
}
