package fatum.catalog.model.vo

import fatum.catalog.model.constant.QuestionType


data class QuestionAnswer(
    private val _question: String,
    val answer: Set<String>
){
    val question = _question.trim()

}
