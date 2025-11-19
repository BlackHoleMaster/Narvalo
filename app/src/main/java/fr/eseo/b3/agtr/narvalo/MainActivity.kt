package fr.eseo.b3.agtr.narvalo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import fr.eseo.b3.agtr.narvalo.ui.theme.NarvaloTheme
import fr.eseo.b3.agtr.narvalo.ui.QuizScreen
import androidx.compose.ui.platform.LocalContext
import fr.eseo.b3.agtr.narvalo.MusicPlayer.MusicPlayerManager


class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                // enableEdgeToEdge() // Note: peut causer des soucis visuels avec certains layouts, activez au besoin.
                setContent {
                    // On récupère le contexte actuel, nécessaire pour le MusicPlayerManager
                    val context = LocalContext.current

                    // 'remember' garantit que le manager n'est créé qu'une seule fois
                    val musicPlayerManager = remember { MusicPlayerManager(context) }

                    // DisposableEffect est crucial : il exécute le code dans `onDispose`
                    // lorsque le composant quitte l'écran (ou que l'app se ferme).
                    // C'est l'équivalent de onDestroy() pour libérer les ressources.
                    DisposableEffect(Unit) {
                        onDispose {
                            musicPlayerManager.release()
                        }
                    }

                    NarvaloTheme {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            // On passe le manager à notre écran
                            QuizScreen(
                                modifier = Modifier.padding(innerPadding),
                                musicPlayerManager = musicPlayerManager // Passage en paramètre
                            )
                        }
                    }
                }
            }
        }

