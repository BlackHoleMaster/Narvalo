package fr.eseo.b3.agtr.narvalo.Question

import com.google.gson.annotations.SerializedName

data class TriviaResponse(
    @SerializedName("response_code")
    val responseCode: Int,
    val results: List<TriviaQuestion>
)

data class TriviaQuestion(
    val category: String,
    val type: String,
    val difficulty: String,
    val question: String,
    @SerializedName("correct_answer")
    val correctAnswer: String,
    @SerializedName("incorrect_answers")
    val incorrectAnswers: List<String>
) {
    fun toQuestion(): Question {
        return Question(
            category = category,
            type = type,
            difficulty = difficulty,
            question = decodeHtml(question),
            correctAnswer = decodeHtml(correctAnswer),
            incorrectAnswers = incorrectAnswers.map { decodeHtml(it) }
        )
    }

    private fun decodeHtml(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&rsquo;", "'")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")
    }
}

