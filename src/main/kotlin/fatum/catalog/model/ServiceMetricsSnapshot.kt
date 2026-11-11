package fatum.catalog.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "SERVICE_METRICS_SNAPSHOT",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_snapshot_service_year_week",
            columnNames = ["SERVICE_ID", "YEAR", "WEEK"]
        )
    ]
)
class ServiceMetricsSnapshot(

    @Column(name = "SERVICE_ID", length = 36, nullable = false)
    val serviceId: String,

    @Column(name = "YEAR", nullable = false)
    val year: Int,

    @Column(name = "WEEK", nullable = false)
    val week: Int,

    @Column(name = "SNAPSHOT_DATE", nullable = false)
    val snapshotDate: LocalDate,

    @Column(name = "VIEWS_IN_PERIOD", nullable = false)
    val viewsInPeriod: Long = 0,

    @Column(name = "DWELL_SECONDS_IN_PERIOD", nullable = false)
    val dwellSecondsInPeriod: Long = 0,

    @Column(name = "DWELL_EVENTS_IN_PERIOD", nullable = false)
    val dwellEventsInPeriod: Int = 0,

    @Column(name = "NEW_ORDERS_IN_PERIOD", nullable = false)
    val newOrdersInPeriod: Int = 0,

    @Column(name = "FAVORITE_COUNT", nullable = false)
    val favoriteCount: Int = 0,

    @Column(name = "TOTAL_ORDERS", nullable = false)
    val totalOrders: Int = 0,

    @Column(name = "PENDING_ORDERS", nullable = false)
    val pendingOrders: Int = 0,

    @Column(name = "ACCEPTED_ORDERS", nullable = false)
    val acceptedOrders: Int = 0,

    @Column(name = "REJECTED_ORDERS", nullable = false)
    val rejectedOrders: Int = 0,

    @Column(name = "DELIVERING_ORDERS", nullable = false)
    val deliveringOrders: Int = 0,

    @Column(name = "COMPLETED_ORDERS", nullable = false)
    val completedOrders: Int = 0,

    @Column(name = "CANCELLED_ORDERS", nullable = false)
    val cancelledOrders: Int = 0,

    @Column(name = "NOT_DELIVERED_ORDERS", nullable = false)
    val notDeliveredOrders: Int = 0,

    @Column(name = "DISPUTE_RELATED_ORDERS", nullable = false)
    val disputeRelatedOrders: Int = 0
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    override fun equals(other: Any?): Boolean {
        if (other !is ServiceMetricsSnapshot) return false
        if (this === other) return true
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}