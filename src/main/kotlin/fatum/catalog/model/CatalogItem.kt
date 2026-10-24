package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.model.vo.FixedQuestion
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import kotlin.collections.MutableList

@Entity
@Table(
    name = "SERVICES",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_services_provider_category",
            columnNames = ["PROVIDER_ID", "CATEGORY"]
        ),
        UniqueConstraint(
            name = "uk_services_provider_title",
            columnNames = ["PROVIDER_ID", "TITLE"]
        )
    ]
)
class CatalogItem(

    @Column(name = "PROVIDER_ID", length = 36, nullable = false)
    val providerId: String,
    title: String,

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    var description: String? = null,

    basePrice: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "PRICE_TYPE", length = 12, nullable = false)
    var priceType: PriceType,

    isNegotiable: Boolean = false,

    defaultEstimatedTime: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "JOB_TYPE", length = 10, nullable = false)
    var jobType: JobType,

    @ManyToOne
    @JoinColumn(name = "CATEGORY", referencedColumnName = "NAME", nullable = false)
    val category: Category,

    @Column(name = "FIXED_QUESTIONS", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var fixedQuestions: Set<FixedQuestion> = emptySet(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "SERVICE_ID", nullable = false)
    var serviceCatalog: MutableList<ServiceImage> = mutableListOf()

) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "TITLE", length = 40, nullable = false)
    var title: String = ""
    set(value) {
        if(value.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        field = toTitle(value)
    }

    @Column(name = "BASE_PRICE", nullable = false)
    var basePrice: BigDecimal = BigDecimal.ZERO
        set(value) {
            if (value < BigDecimal.ZERO) {
                throw FatumServiceException(CatalogErrorCode.NEGATIVE_PRICE)
            }
            field = value
        }

    @Column(name = "IS_NEGOTIABLE", nullable = false)
    var isNegotiable: Boolean = isNegotiable
        protected set

    @Column(name = "DEFAULT_TIME")
    var defaultEstimatedTime: Int? = null
        set(value) {
            value?.let {
                if (it < 0) throw FatumServiceException(CatalogErrorCode.NEGATIVE_ESTIMATED_TIME)
                if (it > 1440) throw FatumServiceException(CatalogErrorCode.MAX_ESTIMATED_TIME)
            }
            field = value
        }


    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 12, nullable = false)
    var status: ServiceStatus = ServiceStatus.ACTIVE


    init {
        if (providerId.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        this.title = title
        this.basePrice = basePrice
        this.defaultEstimatedTime = defaultEstimatedTime

        category.addService(this)
    }

    fun makeNegotiable()  {
        this.isNegotiable = true
    }

    fun makeNonNegotiable() {
        this.isNegotiable = false
    }

    private fun toTitle(newTitle: String): String {
        return newTitle.trim().split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CatalogItem) return false
        if (this === other) return true
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
