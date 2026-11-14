package fatum.catalog.dto.response.analytic

data class LiveMetricsResponse(
    val serviceId: String,
    val totalViews: Long,
    val favoriteCount: Int,
    val totalDwellSeconds: Long,
    val dwellEventCount: Int,
    val averageDwellSeconds: Double
)
