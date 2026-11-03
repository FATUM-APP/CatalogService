package fatum.catalog.dto.response.category

data class CategoryResponse(
    val name: String,
    val slug: String,
    val parent: CategoryResponse? = null,
    val subcategories: Set<CategoryResponse>,
    val schedulingModel: String,
    val isAble: Boolean
)