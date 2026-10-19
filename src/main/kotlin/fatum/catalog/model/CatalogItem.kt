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
    description: String? = null,

    @Column(name = "BASE_PRICE", nullable = false)
    private var _basePrice: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "PRICE_TYPE", length = 12, nullable = false)
    private var _priceType: PriceType,

    @Column(name = "IS_NEGOTIABLE", nullable = false)
    private var _isNegotiable: Boolean = false,

    @Column(name = "DEFAULT_TIME")
    private var _defaultEstimatedTime: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "JOB_TYPE", length = 10, nullable = false)
    private var _jobType: JobType,

    @ManyToOne
    @JoinColumn(name = "CATEGORY", referencedColumnName = "NAME", nullable = false)
    private val category: Category,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "SERVICE_ID", nullable = false)
    private val _serviceCatalog: MutableList<ServiceImage> = mutableListOf(),

    @Column(name = "FIXED_QUESTIONS", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private var _fixedQuestions: Set<FixedQuestion> = emptySet()

) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "TITLE", length = 40, nullable = false)
    private var _title: String = toTitle(title)

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private var _description: String? = description?.trim()

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 12, nullable = false)
    private var _status: ServiceStatus = ServiceStatus.ACTIVE

    init {
        if (providerId.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        if (_basePrice < BigDecimal.ZERO) throw FatumServiceException(CatalogErrorCode.NEGATIVE_PRICE)
        category.addService(this)
    }

    val title: String get() = _title
    val description: String? get() = _description
    val basePrice: BigDecimal get() = _basePrice
    val priceType: PriceType get() = _priceType
    val isNegotiable: Boolean get() = _isNegotiable
    val defaultEstimatedTime: Int? get() = _defaultEstimatedTime
    val status: ServiceStatus get() = _status
    val jobType: JobType get() = _jobType
    val serviceCatalog: List<ServiceImage> get() = _serviceCatalog.toList()
    val fixedQuestions: Set<FixedQuestion> get() = _fixedQuestions

    fun setTitle(newTitle: String) {
        if (newTitle.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        _title = toTitle(newTitle)
    }

    fun setDescription(newDescription: String?) {
        _description = newDescription?.trim()
    }

    fun setPrice(newPrice: BigDecimal) {
        if (newPrice < BigDecimal.ZERO)
            throw FatumServiceException(CatalogErrorCode.NEGATIVE_PRICE)
        _basePrice = newPrice
    }

    fun setPriceType(newPriceType: PriceType) {
        _priceType = newPriceType
    }

    fun makeNegotiable() {
        _isNegotiable = true
    }

    fun makeNonNegotiable() {
        _isNegotiable = false
    }

    fun setDefaultEstimatedTime(newDefaultEstimatedTime: Int?) {
        if (newDefaultEstimatedTime != null && newDefaultEstimatedTime < 0)
            throw FatumServiceException(CatalogErrorCode.NEGATIVE_ESTIMATED_TIME)
        _defaultEstimatedTime = newDefaultEstimatedTime
    }

    fun setStatus(newStatus: ServiceStatus) {
        _status = newStatus
    }

    fun setJobType(newJobType: JobType) {
        _jobType = newJobType
    }

    fun setFixedQuestions(newQuestions: Set<FixedQuestion>?) {
        if (newQuestions == null) clearFixedQuestions()
        else _fixedQuestions = newQuestions
    }

    fun clearFixedQuestions() {
        _fixedQuestions = emptySet()
    }

    fun addImage(image: ServiceImage) {
        _serviceCatalog.add(image)
    }

    fun removeImage(image: ServiceImage) {
        _serviceCatalog.remove(image)
    }

    private fun toTitle(newTitle: String): String {
        return newTitle.trim().split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CatalogItem) return false
        if (this === other) return true
        return this.id == other.id
    }

    override fun hashCode(): Int = this.id?.hashCode() ?: 0
}
