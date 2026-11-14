package fatum.catalog.dto.mapper

import fatum.catalog.dto.response.analytic.AggregatedMetricsResponse
import fatum.catalog.dto.response.analytic.LiveMetricsResponse
import fatum.catalog.dto.response.analytic.MetricsSnapshotResponse
import fatum.catalog.model.ServiceMetrics
import fatum.catalog.model.ServiceMetricsSnapshot
import fatum.catalog.model.constant.MetricsPeriod

fun ServiceMetrics.toLiveResponse(): LiveMetricsResponse =
    LiveMetricsResponse(
        serviceId = this.serviceId,
        totalViews = this.totalViews,
        favoriteCount = this.favoriteCount,
        totalDwellSeconds = this.totalDwellSeconds,
        dwellEventCount = this.dwellEventCount,
        averageDwellSeconds = if (this.dwellEventCount > 0)
            this.totalDwellSeconds.toDouble() / this.dwellEventCount
        else 0.0
    )

fun ServiceMetricsSnapshot.toResponse(): MetricsSnapshotResponse =
    MetricsSnapshotResponse(
        serviceId = this.serviceId,
        year = this.year,
        week = this.week,
        snapshotDate = this.snapshotDate,
        viewsInPeriod = this.viewsInPeriod,
        dwellSecondsInPeriod = this.dwellSecondsInPeriod,
        dwellEventsInPeriod = this.dwellEventsInPeriod,
        newOrdersInPeriod = this.newOrdersInPeriod,
        favoriteCount = this.favoriteCount,
        totalOrders = this.totalOrders,
        pendingOrders = this.pendingOrders,
        acceptedOrders = this.acceptedOrders,
        rejectedOrders = this.rejectedOrders,
        deliveringOrders = this.deliveringOrders,
        completedOrders = this.completedOrders,
        cancelledOrders = this.cancelledOrders,
        notDeliveredOrders = this.notDeliveredOrders,
        disputeRelatedOrders = this.disputeRelatedOrders
    )

fun List<ServiceMetricsSnapshot>.toAggregated(
    serviceId: String?,
    period: MetricsPeriod,
    year: Int,
    periodValue: Int
): AggregatedMetricsResponse {
    if (this.isEmpty()) return emptyAggregated(serviceId, period, year, periodValue)
    val latest = this.last()
    return AggregatedMetricsResponse(
        serviceId = serviceId,
        period = period.name,
        year = year,
        periodValue = periodValue,
        totalViews = this.sumOf { it.viewsInPeriod },
        totalDwellSeconds = this.sumOf { it.dwellSecondsInPeriod },
        totalDwellEvents = this.sumOf { it.dwellEventsInPeriod },
        totalNewOrders = this.sumOf { it.newOrdersInPeriod },
        favoriteCount = latest.favoriteCount,
        totalOrders = latest.totalOrders,
        pendingOrders = latest.pendingOrders,
        acceptedOrders = latest.acceptedOrders,
        rejectedOrders = latest.rejectedOrders,
        deliveringOrders = latest.deliveringOrders,
        completedOrders = latest.completedOrders,
        cancelledOrders = latest.cancelledOrders,
        notDeliveredOrders = latest.notDeliveredOrders,
        disputeRelatedOrders = latest.disputeRelatedOrders,
        snapshotsInPeriod = this.size
    )
}

private fun emptyAggregated(
    serviceId: String?,
    period: MetricsPeriod,
    year: Int,
    periodValue: Int
): AggregatedMetricsResponse =
    AggregatedMetricsResponse(
        serviceId = serviceId,
        period = period.name,
        year = year,
        periodValue = periodValue,
        totalViews = 0,
        totalDwellSeconds = 0,
        totalDwellEvents = 0,
        totalNewOrders = 0,
        favoriteCount = 0,
        totalOrders = 0,
        pendingOrders = 0,
        acceptedOrders = 0,
        rejectedOrders = 0,
        deliveringOrders = 0,
        completedOrders = 0,
        cancelledOrders = 0,
        notDeliveredOrders = 0,
        disputeRelatedOrders = 0,
        snapshotsInPeriod = 0
    )
