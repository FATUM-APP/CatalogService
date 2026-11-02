package fatum.catalog.model.vo

data class QuestionAnswer(
    private val _question: String,
    val answer: Set<String>
) {
    val question = _question.trim()
}
