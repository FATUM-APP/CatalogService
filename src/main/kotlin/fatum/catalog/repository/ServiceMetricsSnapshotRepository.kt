package fatum.catalog.repository

import fatum.catalog.model.ServiceMetricsSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ServiceMetricsSnapshotRepository : JpaRepository<ServiceMetricsSnapshot, String> {

    fun findByServiceIdAndYearAndWeek(serviceId: String, year: Int, week: Int): ServiceMetricsSnapshot?

    @Query("""
        SELECT s FROM ServiceMetricsSnapshot s
        WHERE s.serviceId = :serviceId
          AND s.snapshotDate BETWEEN :startDate AND :endDate
        ORDER BY s.snapshotDate ASC
    """)
    fun findByServiceIdAndDateRange(
        @Param("serviceId") serviceId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<ServiceMetricsSnapshot>

    @Query("""
        SELECT s FROM ServiceMetricsSnapshot s
        WHERE s.serviceId IN :serviceIds
          AND s.snapshotDate BETWEEN :startDate AND :endDate
        ORDER BY s.serviceId, s.snapshotDate ASC
    """)
    fun findByServiceIdsAndDateRange(
        @Param("serviceIds") serviceIds: List<String>,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<ServiceMetricsSnapshot>

    fun findFirstByServiceIdOrderByYearDescWeekDesc(serviceId: String): ServiceMetricsSnapshot?
}
