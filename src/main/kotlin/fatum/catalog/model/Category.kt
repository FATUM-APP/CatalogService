package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import jakarta.persistence.*

@Entity
@Table(name = "CATEGORIES")
class Category private constructor(
    name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "SCHEDULING_MODEL", length = 14, nullable = false)
    val schedulingModel: SchedulingModel,

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
    var isAble: Boolean = true
        protected set

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    private val _subcategories: MutableSet<Category> = mutableSetOf()

    @OneToMany(mappedBy = "category")
    private val _services: MutableSet<CatalogItem> = mutableSetOf()



    init {
        if (name.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        if (parent?.name == this.name)
            throw FatumServiceException(CatalogErrorCode.SELF_PARENT_CATEGORY)
        if (parent?._services?.isNotEmpty() ?: false)
            throw FatumServiceException(CatalogErrorCode.CATEGORY_HAS_SERVICES)
        parent?._subcategories?.add(this)
    }

    /**
     * Root Category constructor
     * @param name
     * @param schedulingModel
     */
    constructor(name: String, schedulingModel: SchedulingModel) : this(name, schedulingModel, null)

    /**
     * Subcategory constructor
     * @param name
     * @param parent
     */
    constructor(name: String, parent: Category) : this(
        name,
        parent.schedulingModel,
        parent
    ) {

    }

    fun hasServices(): Boolean {
        if (_services.isNotEmpty()) return true
        return _subcategories.any { it.hasServices() }
    }

    fun getServices(): Set<CatalogItem> {
        return if (_subcategories.isNotEmpty()) {
            _subcategories.flatMapTo(mutableSetOf()) { it.getServices() }
        } else {
            _services.toSet()
        }
    }

    fun getSubcategories(): Set<Category> = _subcategories.toSet()

    fun removeSubCategory(subcategory: Category) {
        if (subcategory.hasServices())
            throw FatumServiceException(CatalogErrorCode.REMOVING_CATEGORY_WITH_SERVICES)
        _subcategories.remove(subcategory)
    }

    fun addService(catalogItem: CatalogItem) {
        if (_subcategories.isNotEmpty())
            throw FatumServiceException(CatalogErrorCode.CATEGORY_HAS_SUBCATEGORIES)
        _services.add(catalogItem)
    }

    fun disable() {
        this.isAble = false
        _services.filter { it.status == ServiceStatus.ACTIVE }
            .forEach { it.status = ServiceStatus.CATEGORY_DISABLED }
        _subcategories.forEach { it.disable() }

    }

    fun enable() {
        this.isAble = true
        _services.filter { it.status == ServiceStatus.CATEGORY_DISABLED }
            .forEach { it.status = ServiceStatus.ACTIVE }
        _subcategories.forEach { it.enable() }
    }

    private fun toTitle(newCategory: String): String {
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
