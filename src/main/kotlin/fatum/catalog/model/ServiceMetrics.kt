package fatum.catalog.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "SERVICE_METRICS")
class ServiceMetrics(

    @Id
    @Column(name = "SERVICE_ID", length = 36, nullable = false)
    val serviceId: String,

    @Column(name = "TOTAL_VIEWS", nullable = false)
    var totalViews: Long = 0,

    @Column(name = "FAVORITE_COUNT", nullable = false)
    var favoriteCount: Int = 0,

    @Column(name = "TOTAL_DWELL_SECONDS", nullable = false)
    var totalDwellSeconds: Long = 0,

    @Column(name = "DWELL_EVENT_COUNT", nullable = false)
    var dwellEventCount: Int = 0,

    @Column(name = "PERIOD_VIEWS", nullable = false)
    var periodViews: Long = 0,

    @Column(name = "PERIOD_DWELL_SECONDS", nullable = false)
    var periodDwellSeconds: Long = 0,

    @Column(name = "PERIOD_DWELL_EVENTS", nullable = false)
    var periodDwellEvents: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (other !is ServiceMetrics) return false
        if (this === other) return true
        return serviceId == other.serviceId
    }

    override fun hashCode(): Int = serviceId.hashCode()
}