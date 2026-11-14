package fatum.catalog.model.vo

data class QuestionAnswer(
    var question: String,
    val answer: Set<String>
) {
    init{
        question = question.trim()
    }
}
