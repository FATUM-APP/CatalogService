package fatum.catalog.dto

data class CatalogImage(
    val id: String,
    val originalFilename: String,
    val contentType : String,
    val size : Long,
    val url : String
)
