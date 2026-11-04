package fatum.catalog.dto.request.schedule

data class InitializeProviderAvailability(
    private val _providerId: String
){
    val providerId = _providerId.trim()
}
