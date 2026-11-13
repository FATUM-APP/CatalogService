package fatum.catalog.dto.request.order

data class UpdateReason(
    var reason: String
){
    init {
        reason = reason.trim()
    }
}
