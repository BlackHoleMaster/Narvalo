package fr.eseo.b3.agtr.narvalo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import fr.eseo.b3.agtr.narvalo.ui.theme.NarvaloTheme
import fr.eseo.b3.agtr.narvalo.ui.screens.QuizScreen
import fr.eseo.b3.agtr.narvalo.ui.screens.HomeScreen
import fr.eseo.b3.agtr.narvalo.MusicPlayer.MusicPlayerManager
import fr.eseo.b3.agtr.narvalo.data.ScoreManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val musicPlayerManager = remember { MusicPlayerManager(context) }
            val scoreManager = remember { ScoreManager(context) }

            // Charger les scores sauvegardés au démarrage
            val savedScores = remember { scoreManager.loadScores() }

            // État pour la navigation
            var currentScreen by remember { mutableStateOf("home") }
            var highScore by remember { mutableStateOf(savedScores.highScore) }
            var lastScore by remember { mutableStateOf(savedScores.lastScore) }

            DisposableEffect(Unit) {
                onDispose {
                    musicPlayerManager.release()
                }
            }

            NarvaloTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "home" -> HomeScreen(
                            onPlayClick = { currentScreen = "quiz" },
                            modifier = Modifier.padding(innerPadding),
                            highScore = highScore,
                            lastScore = lastScore
                        )
                        "quiz" -> QuizScreen(
                            modifier = Modifier.padding(innerPadding),
                            musicPlayerManager = musicPlayerManager,
                            onQuizComplete = { score ->
                                lastScore = score
                                if (score > highScore) {
                                    highScore = score
                                }
                                // Sauvegarder les scores dans le JSON
                                scoreManager.saveScores(highScore, lastScore)
                                currentScreen = "home"
                            }
                        )
                    }
                }
            }
        }
    }
}

