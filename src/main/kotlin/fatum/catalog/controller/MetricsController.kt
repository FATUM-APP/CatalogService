package fatum.catalog.controller

import fatum.catalog.dto.request.analytic.ServiceViewEventRequest
import fatum.catalog.dto.response.analytic.AggregatedMetricsResponse
import fatum.catalog.dto.response.analytic.LiveMetricsResponse
import fatum.catalog.dto.response.analytic.MetricsSnapshotResponse
import fatum.catalog.model.constant.MetricsPeriod
import fatum.catalog.service.MetricsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metrics")
@Validated
class MetricsController(
    private val metricsService: MetricsService
) {

    @GetMapping("/service/{serviceId}/live")
    fun getLiveMetrics(@PathVariable serviceId: String): ResponseEntity<LiveMetricsResponse> =
        ResponseEntity.ok(metricsService.getLiveMetrics(serviceId))

    @GetMapping("/service/{serviceId}")
    fun getServiceMetrics(
        @PathVariable serviceId: String,
        @RequestParam period: MetricsPeriod,
        @RequestParam year: Int,
        @RequestParam value: Int
    ): ResponseEntity<AggregatedMetricsResponse> =
        ResponseEntity.ok(metricsService.getServiceMetrics(serviceId, period, year, value))

    @GetMapping("/service/{serviceId}/history")
    fun getServiceHistory(
        @PathVariable serviceId: String,
        @RequestParam year: Int
    ): ResponseEntity<List<MetricsSnapshotResponse>> =
        ResponseEntity.ok(metricsService.getServiceHistory(serviceId, year))

    @GetMapping("/provider/{providerId}")
    fun getProviderMetrics(
        @PathVariable providerId: String,
        @RequestParam period: MetricsPeriod,
        @RequestParam year: Int,
        @RequestParam value: Int
    ): ResponseEntity<AggregatedMetricsResponse> =
        ResponseEntity.ok(metricsService.getProviderMetrics(providerId, period, year, value))

    @GetMapping("/provider/{providerId}/services")
    fun getProviderServiceBreakdown(
        @PathVariable providerId: String,
        @RequestParam period: MetricsPeriod,
        @RequestParam year: Int,
        @RequestParam value: Int
    ): ResponseEntity<List<AggregatedMetricsResponse>> =
        ResponseEntity.ok(metricsService.getProviderServiceBreakdown(providerId, period, year, value))

    @PostMapping("/events/view")
    fun trackViewEvent(@RequestBody request: ServiceViewEventRequest): ResponseEntity<Void> {
        metricsService.trackViewEvent(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/snapshots/generate")
    fun generateSnapshots(): ResponseEntity<Void> {
        metricsService.generateAllSnapshots()
        return ResponseEntity.ok().build()
    }
}