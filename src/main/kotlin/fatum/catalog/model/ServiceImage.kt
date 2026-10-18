package fatum.catalog.model

import jakarta.persistence.*
import jakarta.persistence.JoinColumn

@Entity
@Table(name = "SERVICE_PORTAFOLIO")
 class ServiceImage(
    @Column(name = "IMAGE_KEY", nullable = false, length = 512)
    val imageKey: String,

    @Column(name = "ORIGINAL_FILENAME", nullable = false, length = 255)
    val originalFilename: String,

    @Column(name = "CONTENT_TYPE", nullable = false, length = 100)
    val contentType: String,

    @Column(name = "FILE_SIZE", nullable = false)
    val fileSize: Long = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    override fun equals(other: Any?): Boolean {
        if (other !is ServiceImage) return false
        if (this === other) return true
        return this.id == other.id
    }

    override fun hashCode(): Int = this.id?.hashCode() ?: 0
}

