package fr.eseo.b3.agtr.narvalo.Question

data class Question(
    val category: String,
    val type: String,
    val difficulty: String,
    val question: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>
) {
    fun getAllAnswers(): List<String> {
        return (incorrectAnswers + correctAnswer).shuffled()
    }

    fun isCorrectAnswer(answer: String): Boolean {
        return answer == correctAnswer
    }
}