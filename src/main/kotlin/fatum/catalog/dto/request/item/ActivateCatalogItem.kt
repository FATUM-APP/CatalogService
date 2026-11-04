package fatum.catalog.dto.request.item

data class ActivateCatalogItem(
    private val _id: String
){
    val id = _id.trim()
}