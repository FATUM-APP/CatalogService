package fatum.catalog.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "SERVICE_VIEW_EVENT")
class ServiceViewEvent(

    @Column(name = "SERVICE_ID", length = 36, nullable = false)
    val serviceId: String,

    @Column(name = "USER_ID", length = 255, nullable = false)
    val userId: String,

    @Column(name = "DWELL_TIME_SECONDS", nullable = false)
    val dwellTimeSeconds: Int,

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    override fun equals(other: Any?): Boolean {
        if (other !is ServiceViewEvent) return false
        if (this === other) return true
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
