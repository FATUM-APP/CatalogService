package fatum.catalog.model.vo

import fatum.catalog.model.constant.QuestionType

data class FixedQuestion(
    val question: String,
    val type: QuestionType,
    val options: List<String>? = null
)
