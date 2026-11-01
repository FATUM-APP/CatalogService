package fatum.catalog.model.vo

import fatum.catalog.model.constant.QuestionType


data class QuestionAnswer(
    val question: String,
    val type: QuestionType,
    val answer: Set<String>
)
