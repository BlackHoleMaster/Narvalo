package fr.eseo.b3.agtr.narvalo.Question

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.eseo.b3.agtr.narvalo.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuizState {
    object Loading : QuizState()
    data class Success(val questions: List<Question>) : QuizState()
    data class Error(val message: String) : QuizState()
}

class QuizViewModel(private val context: Context) : ViewModel() {
    private val _quizState = MutableStateFlow<QuizState>(QuizState.Loading)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _correctAnswersCount = MutableStateFlow(0)
    val correctAnswersCount: StateFlow<Int> = _correctAnswersCount.asStateFlow()

    fun loadQuestions(difficulty: String? = null) {
        viewModelScope.launch {
            if (difficulty == "emilien") {
                _quizState.value = QuizState.Success(questionsEmilien())
                return@launch
            }

            // Réinitialiser l'état avant de charger de nouvelles questions
            _currentQuestionIndex.value = 0
            _score.value = 0
            _correctAnswersCount.value = 0
            _quizState.value = QuizState.Loading

            var attempt = 0
            val maxAttempts = 5
            var delayMillis = 1000L

            while (attempt < maxAttempts) {
                try {
                    val response = RetrofitInstance.api.getQuestions(
                        amount = 10,
                        difficulty = difficulty?.lowercase(),
                        type = "multiple"
                    )

                    when (response.responseCode) {
                        0 -> {
                            val questions = response.results.map { it.toQuestion() }
                            _quizState.value = QuizState.Success(questions)
                            return@launch
                        }
                        5 -> { // Too many requests - retry with backoff
                            attempt++
                            if (attempt >= maxAttempts) {
                                _quizState.value = QuizState.Error("Trop de requêtes après $maxAttempts tentatives")
                                return@launch
                            }
                            kotlinx.coroutines.delay(delayMillis)
                            delayMillis *= 2
                            continue // Continue to next attempt
                        }
                        1 -> {
                            _quizState.value = QuizState.Error("Pas assez de questions disponibles pour cette difficulté")
                            return@launch
                        }
                        else -> {
                            _quizState.value = QuizState.Error("Erreur lors du chargement des questions (code: ${response.responseCode})")
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    attempt++
                    if (attempt >= maxAttempts) {
                        _quizState.value = QuizState.Error("Erreur réseau: ${e.message}")
                        return@launch
                    }
                    kotlinx.coroutines.delay(delayMillis)
                    delayMillis *= 2
                }
            }
        }
    }

    private fun questionsEmilien(): List<Question> {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.emilien_questions)
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val jsonArray = org.json.JSONArray(jsonString)
            val allQuestions = mutableListOf<Question>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val incorrectAnswers = mutableListOf<String>()
                val incorrectArray = jsonObject.getJSONArray("incorrectAnswers")
                for (j in 0 until incorrectArray.length()) {
                    incorrectAnswers.add(incorrectArray.getString(j))
                }

                allQuestions.add(
                    Question(
                        category = jsonObject.getString("category"),
                        type = jsonObject.getString("type"),
                        difficulty = jsonObject.getString("difficulty"),
                        question = jsonObject.getString("question"),
                        correctAnswer = jsonObject.getString("correctAnswer"),
                        incorrectAnswers = jsonObject.getString("incorrectAnswers").let {
                            val list = mutableListOf<String>()
                            for (k in 0 until incorrectArray.length()) {
                                list.add(incorrectArray.getString(k))
                            }
                            list
                        }
                    )
                )
            }

            allQuestions.shuffled().take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }


    fun answerQuestion(answer: String, correctAnswer: String, difficultyMultiplier: Int) {
        if (answer == correctAnswer) {
            _score.value += 100 * difficultyMultiplier
            _correctAnswersCount.value += 1
        }
    }

    fun nextQuestion() {
        _currentQuestionIndex.value += 1
    }

    fun resetQuiz() {
        _currentQuestionIndex.value = 0
        _score.value = 0
        _correctAnswersCount.value = 0
    }
}

