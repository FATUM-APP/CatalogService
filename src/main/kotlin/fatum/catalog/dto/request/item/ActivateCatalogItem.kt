package fatum.catalog.dto.request.item

data class ActivateCatalogItem(
    var id: String
){
    init{
        id = id.trim()
    }
}