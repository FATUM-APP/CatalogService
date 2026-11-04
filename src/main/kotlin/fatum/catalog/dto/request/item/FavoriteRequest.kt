package fatum.catalog.dto.request.item

data class FavoriteRequest(
    private val _clientId: String,
    private val _serviceId: String
) {
    val clientId = _clientId.trim()
    val serviceId = _serviceId.trim()
}
