package fatum.catalog.model.vo

import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.QuestionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.Locale
import java.util.Locale.getDefault

data class FixedQuestion(
    @field:NotBlank(message = "A Question is required")
    @field:Size(max = 50, message = "A question cannot be longer than 50 characters")
    private val _question: String,
    val type: QuestionType,
    val options: Set<String>? = null
) {
    val question: String = _question.trim().lowercase().replaceFirstChar { it.uppercase() }
    init {
        if ((type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.SINGLE_CHOICE)
            && (options.isNullOrEmpty() || options.size < 2)
            ) throw FatumServiceException(FatumServiceException.QUESTION_WITHOUT_OPTIONS)
    }

}