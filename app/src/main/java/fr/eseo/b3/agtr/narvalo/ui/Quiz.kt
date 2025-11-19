package fr.eseo.b3.agtr.narvalo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.eseo.b3.agtr.narvalo.Question.QuizState
import fr.eseo.b3.agtr.narvalo.Question.QuizViewModel

enum class Difficulty {
    FACILE, MOYEN, DIFFICILE
}

@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = viewModel()
) {
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MOYEN) }
    var isMusicEnabled by remember { mutableStateOf(true) }

    val quizState by viewModel.quizState.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val score by viewModel.score.collectAsState()

    // Charger les questions au démarrage
    LaunchedEffect(selectedDifficulty) {
        val difficulty = when (selectedDifficulty) {
            Difficulty.FACILE -> "easy"
            Difficulty.MOYEN -> "medium"
            Difficulty.DIFFICILE -> "hard"
        }
        viewModel.loadQuestions(difficulty)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = quizState) {
            is QuizState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is QuizState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadQuestions() }) {
                        Text("Réessayer")
                    }
                }
            }
            is QuizState.Success -> {
                val questions = state.questions

                if (currentQuestionIndex < questions.size) {
                    val currentQuestion = questions[currentQuestionIndex]
                    val answers = remember(currentQuestionIndex) {
                        currentQuestion.getAllAnswers()
                    }

                    var selectedAnswer by remember(currentQuestionIndex) { mutableStateOf<String?>(null) }
                    var hasAnswered by remember(currentQuestionIndex) { mutableStateOf(false) }

                    // Passer automatiquement à la question suivante après un délai
                    LaunchedEffect(hasAnswered) {
                        if (hasAnswered) {
                            kotlinx.coroutines.delay(1000)
                            viewModel.nextQuestion()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Barre de difficulté (F, M, D)
                        DifficultyBar(
                            selectedDifficulty = selectedDifficulty,
                            onDifficultySelected = {
                                selectedDifficulty = it
                                viewModel.resetQuiz()
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Compteur de questions
                        Text(
                            text = "${currentQuestionIndex + 1}/${questions.size}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Zone de question
                        QuestionBox(
                            question = currentQuestion.question
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Boutons de réponse
                        AnswersGrid(
                            answers = answers,
                            correctAnswer = currentQuestion.correctAnswer,
                            selectedAnswer = selectedAnswer,
                            hasAnswered = hasAnswered,
                            onAnswerSelected = { answer ->
                                if (!hasAnswered) {
                                    selectedAnswer = answer
                                    hasAnswered = true
                                    viewModel.answerQuestion(answer, currentQuestion.correctAnswer)
                                }
                            }
                        )
                    }
                } else {
                    // Écran de fin de quiz
                    QuizEndScreen(
                        score = score,
                        totalQuestions = questions.size,
                        onRestart = {
                            viewModel.resetQuiz()
                            val difficulty = when (selectedDifficulty) {
                                Difficulty.FACILE -> "easy"
                                Difficulty.MOYEN -> "medium"
                                Difficulty.DIFFICILE -> "hard"
                            }
                            viewModel.loadQuestions(difficulty)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }

        // Bouton de contrôle de musique en bas à droite
        MusicToggleButton(
            isMusicEnabled = isMusicEnabled,
            onToggle = { isMusicEnabled = !isMusicEnabled },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun DifficultyBar(
    selectedDifficulty: Difficulty,
    onDifficultySelected: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(BorderStroke(2.dp, Color.Black)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DifficultyButton(
            text = "F",
            isSelected = selectedDifficulty == Difficulty.FACILE,
            onClick = { onDifficultySelected(Difficulty.FACILE) },
            modifier = Modifier.weight(1f)
        )

        HorizontalDivider(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight(),
            color = Color.Black
        )

        DifficultyButton(
            text = "M",
            isSelected = selectedDifficulty == Difficulty.MOYEN,
            onClick = { onDifficultySelected(Difficulty.MOYEN) },
            modifier = Modifier.weight(1f)
        )

        HorizontalDivider(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight(),
            color = Color.Black
        )

        DifficultyButton(
            text = "D",
            isSelected = selectedDifficulty == Difficulty.DIFFICILE,
            onClick = { onDifficultySelected(Difficulty.DIFFICILE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DifficultyButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.LightGray else Color.White,
            contentColor = Color.Black
        ),
        shape = RectangleShape
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QuestionBox(
    question: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(BorderStroke(2.dp, Color.Black)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = question,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun AnswersGrid(
    answers: List<String>,
    correctAnswer: String,
    selectedAnswer: String?,
    hasAnswered: Boolean,
    onAnswerSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Première ligne avec 2 boutons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnswerButton(
                answer = answers[0],
                label = "A",
                isCorrect = answers[0] == correctAnswer,
                isSelected = answers[0] == selectedAnswer,
                hasAnswered = hasAnswered,
                onClick = { onAnswerSelected(answers[0]) },
                modifier = Modifier.weight(1f)
            )
            AnswerButton(
                answer = answers[1],
                label = "B",
                isCorrect = answers[1] == correctAnswer,
                isSelected = answers[1] == selectedAnswer,
                hasAnswered = hasAnswered,
                onClick = { onAnswerSelected(answers[1]) },
                modifier = Modifier.weight(1f)
            )
        }

        // Deuxième ligne avec 2 boutons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnswerButton(
                answer = answers[2],
                label = "C",
                isCorrect = answers[2] == correctAnswer,
                isSelected = answers[2] == selectedAnswer,
                hasAnswered = hasAnswered,
                onClick = { onAnswerSelected(answers[2]) },
                modifier = Modifier.weight(1f)
            )
            AnswerButton(
                answer = answers[3],
                label = "D",
                isCorrect = answers[3] == correctAnswer,
                isSelected = answers[3] == selectedAnswer,
                hasAnswered = hasAnswered,
                onClick = { onAnswerSelected(answers[3]) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AnswerButton(
    answer: String,
    label: String,
    isCorrect: Boolean,
    isSelected: Boolean,
    hasAnswered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !hasAnswered -> Color.White
        isSelected && isCorrect -> Color(0xFF4CAF50) // Vert pour la bonne réponse
        isSelected && !isCorrect -> Color(0xFFF44336) // Rouge pour la mauvaise réponse
        else -> Color.White
    }

    val textColor = when {
        !hasAnswered -> Color.Black
        isSelected -> Color.White
        else -> Color.Black
    }

    Button(
        onClick = onClick,
        enabled = !hasAnswered,
        modifier = modifier
            .height(100.dp)
            .border(BorderStroke(2.dp, Color.Black)),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = textColor
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = answer,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun QuizEndScreen(
    score: Int,
    totalQuestions: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quiz Terminé !",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Votre score",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$score / $totalQuestions",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = "Recommencer",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun MusicToggleButton(
    isMusicEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onToggle,
        modifier = modifier,
        containerColor = if (isMusicEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = if (isMusicEnabled) "🔊" else "🔇",
            fontSize = 28.sp,
            modifier = Modifier.alpha(if (isMusicEnabled) 1f else 0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuizScreenPreview() {
    QuizScreen()
}
