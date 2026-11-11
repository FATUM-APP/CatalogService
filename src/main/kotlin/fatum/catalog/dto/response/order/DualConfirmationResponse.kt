package fatum.catalog.dto.response.order

data class DualConfirmationResponse(
    val byCustomer: Boolean?,
    val byProvider: Boolean?
)
