package fatum.catalog.dto.request.item

data class FavoriteRequest(
    var clientId: String,
    var serviceId: String
) {
    init {
        clientId = clientId.trim()
        serviceId = serviceId.trim()
    }
}
