package fatum.catalog.dto


import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import java.math.BigDecimal

data class CreateServiceRequest(
    val providerId: String,
    val title: String,
    val description: String,
    val basePice: BigDecimal = BigDecimal.ZERO,
    val priceType: PriceType,
    val isNegotiable: Boolean = false,
    val defaultEstimatedTime: Int? = null,
    val jobType: JobType = JobType.PRESENCIAL,
    val category: String,

    ){

}
