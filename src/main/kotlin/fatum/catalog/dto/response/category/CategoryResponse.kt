package fatum.catalog.dto.response.category

data class CategoryResponse(
    val name: String,
    val slug: String,
    val parentSlug: String? = null,
    val schedulingModel: String,
    val isAble: Boolean,
    val logoUrl: String? = null
)