package fatum.catalog.dto.request

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.vo.QuestionAnswer
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class BookService(
    @field:NotBlank(message = "Client id is required")
    @field:Size(max = 36, message = "Client id must not exceed 36 characters")
    val clientId: String,

    @field:NotNull
    val booking: LocalDateTime,

    @field:NotBlank(message = "Service id is required")
    @field:Size(max = 36, message = "Service id must not exceed 36 characters")
    val serviceId: String,

    @field:PositiveOrZero(message = "Price cannot be negative")
    val negotiatedPrice: BigDecimal? = null,

    @field:Valid
    val answers: Set<QuestionAnswer>,

    val requirements: String? = null
) {
    init {
        val questionFrequencies = answers
            .groupingBy { it.question.trim().lowercase() }
            .eachCount()

        val hasDuplicates = questionFrequencies.any { it.value > 1 }

        if (hasDuplicates)
            throw FatumServiceException(CatalogErrorCode.DUPLICATE_ANSWER)
    }
}