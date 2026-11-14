package fatum.catalog.service

import fatum.catalog.dto.mapper.toAggregated
import fatum.catalog.dto.mapper.toLiveResponse
import fatum.catalog.dto.mapper.toResponse
import fatum.catalog.dto.request.analytic.ServiceViewEventRequest
import fatum.catalog.dto.response.analytic.AggregatedMetricsResponse
import fatum.catalog.dto.response.analytic.LiveMetricsResponse
import fatum.catalog.dto.response.analytic.MetricsSnapshotResponse
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.ServiceMetrics
import fatum.catalog.model.ServiceMetricsSnapshot
import fatum.catalog.model.ServiceViewEvent
import fatum.catalog.model.constant.MetricsPeriod
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.ServiceMetricsRepository
import fatum.catalog.repository.ServiceMetricsSnapshotRepository
import fatum.catalog.repository.ServiceViewEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.WeekFields

@Service
class MetricsService(
    private val metricsRepository: ServiceMetricsRepository,
    private val snapshotRepository: ServiceMetricsSnapshotRepository,
    private val viewEventRepository: ServiceViewEventRepository,
    private val catalogItemService: CatalogItemService,
    private val orderService: OrderService
) {

    // ============================== TRACKING ==============================

    @Transactional
    fun initializeMetrics(serviceId: String) {
        if (!metricsRepository.existsById(serviceId)) {
            metricsRepository.save(ServiceMetrics(serviceId))
        }
    }

    @Transactional
    fun incrementViews(serviceId: String) {
        metricsRepository.incrementViews(serviceId)
    }

    @Transactional
    fun incrementFavorites(serviceId: String) {
        metricsRepository.incrementFavorites(serviceId)
    }

    @Transactional
    fun decrementFavorites(serviceId: String) {
        metricsRepository.decrementFavorites(serviceId)
    }

    @Transactional
    fun trackViewEvent(request: ServiceViewEventRequest) {
        catalogItemService.findById(request.serviceId, ServiceStatus.ACTIVE)

        viewEventRepository.save(
            ServiceViewEvent(
                serviceId = request.serviceId,
                userId = request.userId,
                dwellTimeSeconds = request.dwellTimeSeconds,
                createdAt = request.timestamp
            )
        )
        metricsRepository.addDwellTime(request.serviceId, request.dwellTimeSeconds.toLong())
    }

    // ============================== LIVE QUERIES ==============================

    @Transactional(readOnly = true)
    fun getLiveMetrics(serviceId: String): LiveMetricsResponse =
        metricsRepository.findById(serviceId)
            .orElseThrow { FatumServiceException(CatalogErrorCode.METRICS_NOT_FOUND) }
            .toLiveResponse()

    // ============================== SNAPSHOT QUERIES ==============================

    @Transactional(readOnly = true)
    fun getServiceMetrics(
        serviceId: String,
        period: MetricsPeriod,
        year: Int,
        value: Int
    ): AggregatedMetricsResponse {
        val (startDate, endDate) = getDateRange(period, year, value)
        val snapshots = snapshotRepository.findByServiceIdAndDateRange(serviceId, startDate, endDate)
        return snapshots.toAggregated(serviceId, period, year, value)
    }

    @Transactional(readOnly = true)
    fun getServiceHistory(serviceId: String, year: Int): List<MetricsSnapshotResponse> {
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)
        return snapshotRepository.findByServiceIdAndDateRange(serviceId, startDate, endDate)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getProviderMetrics(
        providerId: String,
        period: MetricsPeriod,
        year: Int,
        value: Int
    ): AggregatedMetricsResponse {
        val serviceIds = catalogItemService.findByProviderAndCategory(providerId).map { it.id!! }
        if (serviceIds.isEmpty()) return emptyAggregated(null, period, year, value)

        val (startDate, endDate) = getDateRange(period, year, value)
        val snapshots = snapshotRepository.findByServiceIdsAndDateRange(serviceIds, startDate, endDate)
        return snapshots.toAggregated(null, period, year, value)
    }

    @Transactional(readOnly = true)
    fun getProviderServiceBreakdown(
        providerId: String,
        period: MetricsPeriod,
        year: Int,
        value: Int
    ): List<AggregatedMetricsResponse> {
        val serviceIds = catalogItemService.findByProviderAndCategory(providerId).map { it.id!! }
        if (serviceIds.isEmpty()) return emptyList()

        val (startDate, endDate) = getDateRange(period, year, value)
        val snapshots = snapshotRepository.findByServiceIdsAndDateRange(serviceIds, startDate, endDate)
        return snapshots.groupBy { it.serviceId }
            .map { (svcId, snaps) -> snaps.toAggregated(svcId, period, year, value) }
    }

    // ============================== SNAPSHOT GENERATION ==============================

    fun generateAllSnapshots() {
        val serviceIds = catalogItemService.findAll().map { it.id!! }
        serviceIds.forEach { generateSnapshotForService(it) }
    }

    @Transactional
    fun generateSnapshotForService(serviceId: String) {
        val metrics = metricsRepository.findById(serviceId).orElse(null) ?: return

        val now = LocalDate.now()
        val weekFields = WeekFields.ISO
        val year = now.get(weekFields.weekBasedYear())
        val week = now.get(weekFields.weekOfWeekBasedYear())

        if (snapshotRepository.findByServiceIdAndYearAndWeek(serviceId, year, week) != null) return

        val weekStart = now.with(weekFields.dayOfWeek(), 1).atStartOfDay()
        val weekEnd = now.with(weekFields.dayOfWeek(), 7).atTime(23, 59, 59)

        val statusMap = orderService.countByServiceGroupedByStatus(serviceId)
        val totalOrders = orderService.countByService(serviceId)
        val newOrdersInPeriod = orderService.countNewOrdersInPeriod(serviceId, weekStart, weekEnd)

        val snapshot = ServiceMetricsSnapshot(
            serviceId = serviceId,
            year = year,
            week = week,
            snapshotDate = now,
            viewsInPeriod = metrics.periodViews,
            dwellSecondsInPeriod = metrics.periodDwellSeconds,
            dwellEventsInPeriod = metrics.periodDwellEvents,
            newOrdersInPeriod = newOrdersInPeriod.toInt(),
            favoriteCount = metrics.favoriteCount,
            totalOrders = totalOrders.toInt(),
            pendingOrders = statusMap.getOrDefault(OrderStatus.PENDING, 0),
            acceptedOrders = statusMap.getOrDefault(OrderStatus.ACCEPTED, 0),
            rejectedOrders = statusMap.getOrDefault(OrderStatus.REJECTED, 0),
            deliveringOrders = statusMap.getOrDefault(OrderStatus.DELIVERING, 0),
            completedOrders = statusMap.getOrDefault(OrderStatus.COMPLETED, 0),
            cancelledOrders = statusMap.getOrDefault(OrderStatus.CANCELLED, 0),
            notDeliveredOrders = statusMap.getOrDefault(OrderStatus.NOT_DELIVERED, 0),
            disputeRelatedOrders = (statusMap.getOrDefault(OrderStatus.DISPUTED, 0)
                    + statusMap.getOrDefault(OrderStatus.SOLVED, 0)
                    + statusMap.getOrDefault(OrderStatus.REFUNDED, 0))
        )

        snapshotRepository.save(snapshot)
        metricsRepository.resetPeriodCounters(serviceId)
    }

    // ============================== HELPERS ==============================

    private fun getDateRange(period: MetricsPeriod, year: Int, value: Int): Pair<LocalDate, LocalDate> =
        when (period) {
            MetricsPeriod.WEEK -> {
                val weekFields = WeekFields.ISO
                val start = LocalDate.ofYearDay(year, 1)
                    .with(weekFields.weekOfWeekBasedYear(), value.toLong())
                    .with(weekFields.dayOfWeek(), 1)
                val end = start.plusDays(6)
                start to end
            }
            MetricsPeriod.MONTH -> {
                val start = LocalDate.of(year, value, 1)
                val end = start.withDayOfMonth(start.lengthOfMonth())
                start to end
            }
            MetricsPeriod.SEMESTER -> {
                val startMonth = if (value == 1) 1 else 7
                val endMonth = if (value == 1) 6 else 12
                val start = LocalDate.of(year, startMonth, 1)
                val end = LocalDate.of(year, endMonth, 1)
                    .withDayOfMonth(LocalDate.of(year, endMonth, 1).lengthOfMonth())
                start to end
            }
            MetricsPeriod.YEAR -> {
                LocalDate.of(year, 1, 1) to LocalDate.of(year, 12, 31)
            }
        }

    private fun emptyAggregated(
        serviceId: String?,
        period: MetricsPeriod,
        year: Int,
        value: Int
    ): AggregatedMetricsResponse =
        emptyList<ServiceMetricsSnapshot>().toAggregated(serviceId, period, year, value)
}