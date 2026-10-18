package fatum.catalog.model

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
    description: String,

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
    private var _jobType: JobType = JobType.PRESENCIAL,

    @ManyToOne
    @JoinColumn(name = "CATEGORY", referencedColumnName = "NAME", nullable = false)
    private val category: Category,

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _serviceCatalog: MutableList<ServiceImage> = mutableListOf(),

    @Column(name = "FIXED_QUESTIONS", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private var _fixedQuestions: MutableList<FixedQuestion> = mutableListOf()

    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "TITLE", length = 40, nullable = false)
    private var _title: String = toTitle(title)

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", nullable = false)
    private var _description: String = description.trim()

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 12, nullable = false)
    private var _status: ServiceStatus = ServiceStatus.ACTIVE


    init {
        if(_basePrice < BigDecimal.ZERO) throw FatumServiceException(FatumServiceException.NEGATIVE_PRICE)
        category.addService(this)
    }

    val title: String get() = _title
    val description: String get() = _description
    val basePrice: BigDecimal get() = _basePrice
    val priceType: PriceType get() = _priceType
    val isNegotiable: Boolean get() = _isNegotiable
    val defaultEstimatedTime: Int? get() = _defaultEstimatedTime
    val status: ServiceStatus get() = _status
    val jobType: JobType get() = _jobType
    val serviceCatalog: List<ServiceImage> get() = _serviceCatalog.toList()
    val fixedQuestions: List<FixedQuestion> get() = _fixedQuestions.toList()

    fun changeTitle(newTitle: String) {
        _title = toTitle(newTitle)
    }

    fun changeDescription(newDescription: String) {
        _description = newDescription.trim()
    }

    fun changePrice(newPrice: BigDecimal) {
        _basePrice = newPrice
    }

    fun changePriceType(newPriceType: PriceType) {
        _priceType = newPriceType
    }

    fun changeNegotiable(newNegotiable: Boolean) {
        _isNegotiable = newNegotiable
    }

    fun changeDefaultEstimatedTime(newDefaultEstimatedTime: Int?) {
        _defaultEstimatedTime = newDefaultEstimatedTime
    }
    fun changeStatus(newStatus: ServiceStatus) {
        _status = newStatus
    }

    fun changeJobType(newJobType: JobType) {
        _jobType = newJobType
    }

    fun addServiceImage(serviceImage: ServiceImage) {
        _serviceCatalog.add(serviceImage)
    }

    fun removeServiceImage(serviceImage: ServiceImage) {
        _serviceCatalog.remove(serviceImage)
    }

    fun addFixedQuestion(fixedQuestion: FixedQuestion) {
        _fixedQuestions.add(fixedQuestion)
    }
    fun removeFixedQuestion(fixedQuestion: FixedQuestion) {
        _fixedQuestions.remove(fixedQuestion)
    }

    private fun toTitle(newTitle: String): String{
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