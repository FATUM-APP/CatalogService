package fatum.catalog.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "CLIENT_FAVORITES",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_favorites_client_service",
            columnNames = ["CLIENT_ID", "SERVICE_ID"]
        )
    ]
)
class ClientFavorite(
    @Column(name = "CLIENT_ID", length = 36, nullable = false)
    val clientId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SERVICE_ID", nullable = false)
    val catalogItem: CatalogItem
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    override fun equals(other: Any?): Boolean {
        if (other !is ClientFavorite) return false
        if (this === other) return true
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
