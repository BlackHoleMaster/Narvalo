package fr.eseo.b3.agtr.narvalo.Question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuizState {
    object Loading : QuizState()
    data class Success(val questions: List<Question>) : QuizState()
    data class Error(val message: String) : QuizState()
}

class QuizViewModel : ViewModel() {
    private val _quizState = MutableStateFlow<QuizState>(QuizState.Loading)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    fun loadQuestions(difficulty: String? = null) {
        viewModelScope.launch {
            try {
                _quizState.value = QuizState.Loading
                val response = RetrofitInstance.api.getQuestions(
                    amount = 10,
                    difficulty = difficulty?.lowercase(),
                    type = "multiple"
                )

                if (response.responseCode == 0) {
                    val questions = response.results.map { it.toQuestion() }
                    _quizState.value = QuizState.Success(questions)
                    _currentQuestionIndex.value = 0
                    _score.value = 0
                } else {
                    _quizState.value = QuizState.Error("Erreur lors du chargement des questions")
                }
            } catch (e: Exception) {
                _quizState.value = QuizState.Error("Erreur réseau: ${e.message}")
            }
        }
    }

    fun answerQuestion(answer: String, correctAnswer: String) {
        if (answer == correctAnswer) {
            _score.value += 1
        }
    }

    fun nextQuestion() {
        _currentQuestionIndex.value += 1
    }

    fun resetQuiz() {
        _currentQuestionIndex.value = 0
        _score.value = 0
    }
}

