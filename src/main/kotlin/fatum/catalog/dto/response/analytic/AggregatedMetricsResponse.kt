package fatum.catalog.dto.response.analytic

data class AggregatedMetricsResponse(
    val serviceId: String?,
    val period: String,
    val year: Int,
    val periodValue: Int,
    val totalViews: Long,
    val totalDwellSeconds: Long,
    val totalDwellEvents: Int,
    val totalNewOrders: Int,
    val favoriteCount: Int,
    val totalOrders: Int,
    val pendingOrders: Int,
    val acceptedOrders: Int,
    val rejectedOrders: Int,
    val deliveringOrders: Int,
    val completedOrders: Int,
    val cancelledOrders: Int,
    val notDeliveredOrders: Int,
    val disputeRelatedOrders: Int,
    val snapshotsInPeriod: Int
)
