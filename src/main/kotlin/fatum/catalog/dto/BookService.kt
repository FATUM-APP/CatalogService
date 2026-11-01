package fatum.catalog.dto

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.vo.QuestionAnswer
import jakarta.validation.Valid
import jakarta.validation.constraints.Negative
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDateTime

data class BookService(
    @field:NotBlank(message = "Client id is required")
    val clientId: String,

    val booking: LocalDateTime,

    val serviceId: String,

    @field:Negative(message = "Price cannot be negative")
    val negotiatedPrice: BigDecimal? = null,

    @field:Valid
    val answers: Set<QuestionAnswer>,

    val requirements: String? = null
){
    init {
        val questionFrequencies = answers
            .groupingBy { it.question.trim().lowercase() }
            .eachCount()


        val hasDuplicates = questionFrequencies.any { it.value > 1 }

        if (hasDuplicates)
            throw FatumServiceException(CatalogErrorCode.DUPLICATE_ANSWER)

    }
}