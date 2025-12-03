package fr.eseo.b3.agtr.narvalo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.eseo.b3.agtr.narvalo.Question.QuizState
import fr.eseo.b3.agtr.narvalo.Question.QuizViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import fr.eseo.b3.agtr.narvalo.MusicPlayer.MusicPlayerManager
import androidx.lifecycle.ViewModelProvider

import fr.eseo.b3.agtr.narvalo.R
import fr.eseo.b3.agtr.narvalo.data.ScoreManager
import kotlinx.coroutines.delay

enum class Difficulty {
    FACILE, MOYEN, DIFFICILE, EMILIEN
}

@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = run {
        val context = LocalContext.current
        viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return QuizViewModel(context) as T
                }
            }
        )
    },
    musicPlayerManager: MusicPlayerManager,
    onQuizComplete: (Int) -> Unit = {}
) {
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MOYEN) }
    var isPlaying by remember { mutableStateOf(musicPlayerManager.isPlaying)}
    val quizState by viewModel.quizState.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val score by viewModel.score.collectAsState()
    val correctAnswersCount by viewModel.correctAnswersCount.collectAsState()
    val context = LocalContext.current
    val scoreManager = remember { ScoreManager(context) }
    // Make high score reactive so the UI updates when ScoreManager changes
    var highScore by remember { mutableStateOf(scoreManager.getHighScore()) }

    // A simple key to force full UI recomposition when switching difficulty (useful for EMILIEN)
    var uiRefreshKey by remember { mutableStateOf(0) }

    // Calculer le multiplicateur de difficulté
    val difficultyMultiplier = when (selectedDifficulty) {
        Difficulty.FACILE -> 1
        Difficulty.MOYEN -> 3
        Difficulty.DIFFICILE -> 5
        Difficulty.EMILIEN -> 100    }

    val musicUrls = mapOf(
        Difficulty.FACILE to R.raw.c418,
        Difficulty.MOYEN to R.raw.nightcity,
        Difficulty.DIFFICILE to R.raw.soulofcinder,
        Difficulty.EMILIEN to R.raw.the_only_thing_they_fear_is_you
    )

    // Wrap the whole quiz in a key that includes uiRefreshKey so we can force a full recomposition
    key(selectedDifficulty, uiRefreshKey) {

        // Charger les questions au démarrage
        LaunchedEffect(selectedDifficulty) {
            val difficulty = when (selectedDifficulty) {
                Difficulty.FACILE -> "easy"
                Difficulty.MOYEN -> "medium"
                Difficulty.DIFFICILE -> "hard"
                Difficulty.EMILIEN -> "emilien"
            }
            viewModel.resetQuiz()
            viewModel.loadQuestions(difficulty)
        }
        LaunchedEffect(selectedDifficulty, isPlaying) {
            if (isPlaying) {
                // On récupère l'identifiant de la ressource musicale locale
                val resId = musicUrls[selectedDifficulty]
                if (resId != null) {
                    // On appelle la méthode pour la musique LOCALE
                    musicPlayerManager.playLocalMusic(resId, isLooping = true)
                }
            } else {
                // Si la musique est désactivée, on l'arrête
                musicPlayerManager.stop()
            }
        }

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Image de fond
            Image(
                painter = painterResource(id = R.drawable.background_quiz),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

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
                                delay(1000)
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
                                score = highScore,
                                selectedDifficulty = selectedDifficulty,
                                onDifficultySelected = { newDifficulty ->
                                    // Immediately reset score and answered count in the ViewModel
                                    viewModel.resetQuiz()
                                    // Force a full UI refresh when switching difficulty (EMILIEN needs a full refresh)
                                    uiRefreshKey += 1
                                    // Finally set the selected difficulty
                                    selectedDifficulty = newDifficulty
                                },
                                enabled = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Compteur de questions et score
                            Column(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${currentQuestionIndex + 1}/${questions.size}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "Score: $score pts",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

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
                                        viewModel.answerQuestion(answer, currentQuestion.correctAnswer, difficultyMultiplier)
                                    }
                                }
                            )
                        }
                    } else {
                        // Mise à jour du high score / last score lorsque le quiz est terminé
                        LaunchedEffect(key1 = score) {
                            // Mettre à jour le dernier score et le high score dans ScoreManager
                            scoreManager.updateLastScore(score)
                            // Lire la nouvelle valeur de high score
                            highScore = scoreManager.getHighScore()
                        }
                        // Écran de fin de quiz
                        QuizEndScreen(
                            score = score,
                            correctAnswers = correctAnswersCount,
                            totalQuestions = questions.size,
                            onRestart = {
                                viewModel.resetQuiz()
                                val difficulty = when (selectedDifficulty) {
                                    Difficulty.FACILE -> "easy"
                                    Difficulty.MOYEN -> "medium"
                                    Difficulty.DIFFICILE -> "hard"
                                    Difficulty.EMILIEN -> "emilien"
                                }
                                viewModel.loadQuestions(difficulty)
                            },
                            onBackToHome = {
                                onQuizComplete(score)
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
                isMusicEnabled = isPlaying,
                onToggle = { isPlaying = !isPlaying }, // On change l'état 'isPlaying'
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun DifficultyBar(
    modifier: Modifier = Modifier,
    score: Int = 0,
    selectedDifficulty: Difficulty,
    onDifficultySelected: (Difficulty) -> Unit,
    enabled: Boolean = true
) {
    val barShape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = barShape,
        color = Color.White.copy(alpha = 0.95f),
        border = BorderStroke(2.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DifficultyButton(
                text = "F",
                isSelected = selectedDifficulty == Difficulty.FACILE,
                onClick = { onDifficultySelected(Difficulty.FACILE) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                useRoundedShape = false
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
                modifier = Modifier.weight(1f),
                enabled = enabled,
                useRoundedShape = false
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
                modifier = Modifier.weight(1f),
                enabled = enabled,
                useRoundedShape = false
            )
        }
    }
    if (score >= 3000) {// Désactivé si le score est inférieur à 5000
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(BorderStroke(2.dp, Color.Black), shape = barShape)
                .alpha(0.95f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DifficultyButton(
                text = "EMILIEN",
                isSelected = selectedDifficulty == Difficulty.EMILIEN,
                onClick = { onDifficultySelected(Difficulty.EMILIEN) },
                modifier = Modifier.weight(1f),
                enabled = true
            )
        }
    }
}

@Composable
fun DifficultyButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    useRoundedShape: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxHeight(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.LightGray else Color.Transparent,
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        ),
        // Use rectangle shape when inside rounded container, rounded when standalone
        shape = if (useRoundedShape) RoundedCornerShape(12.dp) else androidx.compose.ui.graphics.RectangleShape
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
    val questionShape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(BorderStroke(2.dp, Color.Black), shape = questionShape),
        color = Color.White.copy(alpha = 0.95f),
        shape = questionShape
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = question,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
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
                answer = "A : " + answers[0],
                isCorrect = answers[0] == correctAnswer,
                isSelected = answers[0] == selectedAnswer,
                hasAnswered = hasAnswered,
                onClick = { onAnswerSelected(answers[0]) },
                modifier = Modifier.weight(1f),

            )
            AnswerButton(
                answer = "B : " + answers[1],
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
                answer = "C : " + answers[2],
                isCorrect = answers[2] == correctAnswer,
                isSelected = answers[2] == selectedAnswer,
                hasAnswered = hasAnswered,
                onClick = { onAnswerSelected(answers[2]) },
                modifier = Modifier.weight(1f)
            )
            AnswerButton(
                answer = "D : " + answers[3],
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
    isCorrect: Boolean,
    isSelected: Boolean,
    hasAnswered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !hasAnswered -> Color.White.copy(alpha = 0.95f)
        isCorrect -> Color(0xFF4CAF50) // Vert pour la bonne réponse (toujours)
        !isCorrect -> Color(0xFFF44336) // Rouge pour TOUTES les mauvaises réponses quand on a répondu
        else -> Color.White.copy(alpha = 0.95f)
    }

    val textColor = when {
        !hasAnswered -> Color.Black
        isCorrect -> Color.White // Texte blanc pour la bonne réponse
        !isCorrect -> Color.White // Texte blanc pour TOUTES les mauvaises réponses
        else -> Color.Black
    }

    val buttonShape = RoundedCornerShape(12.dp)
    Button(
        onClick = onClick,
        enabled = !hasAnswered,
        modifier = modifier
            .height(100.dp)
            .border(BorderStroke(2.dp, Color.Black), shape = buttonShape),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = textColor
        ),
        shape = buttonShape,
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
    modifier: Modifier = Modifier,
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    onRestart: () -> Unit,
    onBackToHome: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Quiz Terminé !",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Score en points
            Text(
                text = "Votre score",
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$score points",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nombre de bonnes réponses
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✓ $correctAnswers / $totalQuestions bonnes réponses",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Bouton Recommencer
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

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton Menu
            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    text = "Menu",
                    fontSize = 20.sp
                )
            }
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
    // Preview: create a real MusicPlayerManager if possible; keep it simple for preview
    val context = LocalContext.current
    QuizScreen(musicPlayerManager = MusicPlayerManager(context))
}
