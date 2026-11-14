package fatum.catalog.dto.request.item

data class CatalogImage(
    val id: String,
    val originalFilename: String,
    val contentType : String,
    val size : Long,
    val url : String
)