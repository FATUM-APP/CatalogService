package fatum.catalog.model

import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.SchedulingModel
import jakarta.persistence.*

@Entity
@Table(name = "CATEGORIES")
class Category(
    name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "SCHEDULING_MODEL", length = 14, nullable = false)
    private var _schedulingModel: SchedulingModel? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_NAME", referencedColumnName = "NAME")
    val parent: Category? = null
) {

    @Id
    @Column(name = "NAME", length = 30, nullable = false, unique = true, updatable = false)
    val name: String = toTitle(name)

    @Column(name = "SLUG", length = 30, nullable = false, updatable = false)
    val slug: String = this.name.lowercase().replace(" ", "-")

    @Column(name = "IS_ABLE", nullable = false)
    private var _isAble: Boolean = true

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    private val _subcategories: MutableList<Category> = mutableListOf()

    @OneToMany(mappedBy = "category", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    private val _services: MutableList<OfferedService> = mutableListOf()

    val offeredServices: List<OfferedService> get() = _services.toList()
    val subcategories: List<Category> get() = _subcategories.toList()
    val isAble: Boolean get() = _isAble
    val schedulingModel: SchedulingModel? get() = _schedulingModel

    init {
        if (name.isBlank()) throw FatumServiceException(FatumServiceException.BLANK)
        if (parent?.name == this.name)
            throw FatumServiceException(FatumServiceException.SELF_PARENT_CATEGORY)
        if (this.parent?._services?.isNotEmpty() ?: false)
            throw FatumServiceException(FatumServiceException.CATEGORY_HAS_SERVICES)
        _schedulingModel = parent?.schedulingModel ?: _schedulingModel
                ?: throw FatumServiceException(FatumServiceException.CATEGORY_WITHOUT_SCHEDULING)
        parent?._subcategories?.add(this)
    }

    fun removeSubCategory(subcategory: Category) {
        this._subcategories.remove(subcategory)
    }

    fun addService(offeredService: OfferedService) {
        if (_subcategories.isNotEmpty())
            throw FatumServiceException(FatumServiceException.CATEGORY_HAS_SUBCATEGORIES)
        this._services.add(offeredService)
    }

    fun removeService(offeredService: OfferedService) {
        this._services.remove(offeredService)
    }

    fun disable() {
        _isAble = false
        _subcategories.forEach { it.disable() }  // cascade down
    }

    fun enable() {
        _isAble = true
        _subcategories.forEach { it.enable() }  // cascade up
    }

    private fun toTitle(newCategory: String): String{
        return newCategory.trim().split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }



    override fun equals(other: Any?): Boolean {
        if (other !is Category) return false
        if (this === other) return true
        return this.name == other.name
    }

    override fun hashCode(): Int = this.name.hashCode()
}