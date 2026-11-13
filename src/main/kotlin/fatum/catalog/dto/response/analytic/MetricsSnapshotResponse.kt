package fatum.catalog.dto.response.analytic

import java.time.LocalDate

data class MetricsSnapshotResponse(
    val serviceId: String,
    val year: Int,
    val week: Int,
    val snapshotDate: LocalDate,
    val viewsInPeriod: Long,
    val dwellSecondsInPeriod: Long,
    val dwellEventsInPeriod: Int,
    val newOrdersInPeriod: Int,
    val favoriteCount: Int,
    val totalOrders: Int,
    val pendingOrders: Int,
    val acceptedOrders: Int,
    val rejectedOrders: Int,
    val deliveringOrders: Int,
    val completedOrders: Int,
    val cancelledOrders: Int,
    val notDeliveredOrders: Int,
    val disputeRelatedOrders: Int
)
