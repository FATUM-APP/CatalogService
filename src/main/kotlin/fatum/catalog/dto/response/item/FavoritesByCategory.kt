package fatum.catalog.dto.response.item

data class FavoritesByCategory(
    val category: String,
    val categorySlug: String,
    val services: List<ItemResponse>
)
