package fatum.catalog.repository

import fatum.catalog.model.ServiceMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ServiceMetricsRepository : JpaRepository<ServiceMetrics, String> {

    @Modifying
    @Query("""
        UPDATE ServiceMetrics m
        SET m.totalViews = m.totalViews + 1, m.periodViews = m.periodViews + 1
        WHERE m.serviceId = :serviceId
    """)
    fun incrementViews(@Param("serviceId") serviceId: String)

    @Modifying
    @Query("""
        UPDATE ServiceMetrics m
        SET m.favoriteCount = m.favoriteCount + 1
        WHERE m.serviceId = :serviceId
    """)
    fun incrementFavorites(@Param("serviceId") serviceId: String)

    @Modifying
    @Query("""
        UPDATE ServiceMetrics m
        SET m.favoriteCount = m.favoriteCount - 1
        WHERE m.serviceId = :serviceId AND m.favoriteCount > 0
    """)
    fun decrementFavorites(@Param("serviceId") serviceId: String)

    @Modifying
    @Query("""
        UPDATE ServiceMetrics m
        SET m.totalDwellSeconds = m.totalDwellSeconds + :seconds,
            m.dwellEventCount = m.dwellEventCount + 1,
            m.periodDwellSeconds = m.periodDwellSeconds + :seconds,
            m.periodDwellEvents = m.periodDwellEvents + 1
        WHERE m.serviceId = :serviceId
    """)
    fun addDwellTime(@Param("serviceId") serviceId: String, @Param("seconds") seconds: Long)

    @Modifying
    @Query("""
        UPDATE ServiceMetrics m
        SET m.periodViews = 0, m.periodDwellSeconds = 0, m.periodDwellEvents = 0
        WHERE m.serviceId = :serviceId
    """)
    fun resetPeriodCounters(@Param("serviceId") serviceId: String)
}