package fr.eseo.b3.agtr.narvalo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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

    // Calcul the difficulty multiplier
    val difficultyMultiplier = when (selectedDifficulty) {
        Difficulty.FACILE -> 1
        Difficulty.MOYEN -> 3
        Difficulty.DIFFICILE -> 5
        Difficulty.EMILIEN -> 100    }

    val musicUrls = mapOf(
        Difficulty.FACILE to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        Difficulty.MOYEN to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
        Difficulty.DIFFICILE to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3",
        Difficulty.EMILIEN to "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1"
    )

    // Wrap the whole quiz in a key that includes uiRefreshKey so we can force a full recomposition
    key(selectedDifficulty, uiRefreshKey) {

        // Load the questions when the screen starts
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
                // We recover the resource ID of the local music
                val resId = musicUrls[selectedDifficulty]
                if (resId != null) {
                    // We call the method for the LOCAL music
                    musicPlayerManager.playLocalMusic(resId, isLooping = true)
                }
            } else {
                //  If the music is disabled, we stop it
                musicPlayerManager.stop()
            }
        }

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            //  Wallpaper
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

                        // To pass automatically to the next question after a delay
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
                            // Navbar of difficulty (F, M, D)
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

                            // Counter of questions and score
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
                                AnimatedScore(score = score)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                        // Question area
                        QuestionBox(
                            question = currentQuestion.question
                        )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Button answer
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
                        // Update the last score / high score when the quiz finished
                        LaunchedEffect(key1 = score) {
                            // Update the last score and high score in ScoreManager

                            scoreManager.updateLastScore(score)
                            //  Read the new high score from ScoreManager

                            highScore = scoreManager.getHighScore()
                        }
                        // Screen ending for the quiz
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

            //  Button of control of music at the bottom right

            MusicToggleButton(
                isMusicEnabled = isPlaying,
                onToggle = { isPlaying = !isPlaying }, //  We change the state 'isPlaying'
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun AnimatedScore(score: Int) {
    var animatedScore by remember { mutableIntStateOf(0) }

    LaunchedEffect(score) {
        animate(
            initialValue = animatedScore.toFloat(),
            targetValue = score.toFloat(),
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedScore = value.toInt()
        }
    }

    Surface(
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Score: $animatedScore pts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
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
    if (score >= 3000) {//  Disable if score less than 5000
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
        shape = if (useRoundedShape) RoundedCornerShape(12.dp) else RectangleShape
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
        shape = questionShape,
        shadowElevation = 8.dp
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
        // First line with 2 buttons
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

        // Second line with 2 buttons
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
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }
    var shouldShake by remember { mutableStateOf(false) }

    // Collect press state for scale animation
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    // Trigger shake animation for wrong selected answer
    LaunchedEffect(hasAnswered, isCorrect, isSelected) {
        if (hasAnswered && !isCorrect && isSelected) {
            shouldShake = true
            delay(500)
            shouldShake = false
        }
    }

    // Shake animation
    val offsetX by animateFloatAsState(
        targetValue = if (shouldShake) 10f else 0f,
        animationSpec = repeatable(
            iterations = 3,
            animation = tween(50),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeAnimation"
    )

    // Scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !hasAnswered) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scaleAnimation"
    )

    val backgroundColor = when {
        !hasAnswered -> Color.White.copy(alpha = 0.95f)
        isCorrect -> Color(0xFF4CAF50)
        !isCorrect -> Color(0xFFF44336)
        else -> Color.White.copy(alpha = 0.95f)
    }

    val textColor = when {
        !hasAnswered -> Color.Black
        isCorrect -> Color.White
        !isCorrect -> Color.White
        else -> Color.Black
    }

    val buttonShape = RoundedCornerShape(12.dp)
    Button(
        onClick = onClick,
        enabled = !hasAnswered,
        interactionSource = interactionSource,
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .offset(x = offsetX.dp)
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

            // Score
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

            // Numbre of right answers
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

            //  Restart Button
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

            //  Menu Button
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
