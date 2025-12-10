// Fichier: MusicPlayer/MusicPlayerManager.kt
package fr.eseo.b3.agtr.narvalo.MusicPlayer

import android.content.Context
import android.media.AudioAttributes // ✅ CORRECTION : Importez la bonne classe
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log

class MusicPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    private var currentResource: String? = null

    // --- GESTION DU FOCUS AUDIO ---

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("MusicPlayerManager", "Focus audio perdu définitivement. Arrêt de la musique.")
                if (isPlaying) {
                    stop()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying) {
                    Log.d("MusicPlayerManager", "Focus audio perdu temporairement. Mise en pause.")
                    mediaPlayer?.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying) {
                    Log.d("MusicPlayerManager", "Duck: réduction du volume.")
                    mediaPlayer?.setVolume(0.3f, 0.3f)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("MusicPlayerManager", "Focus audio regagné. Reprise de la lecture.")
                mediaPlayer?.setVolume(1.0f, 1.0f)
                // On ne redémarre que s'il a été mis en pause, sinon on attend la préparation du streaming.
                // mediaPlayer?.start()
            }
        }
    }

    private val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                // ✅ CORRECTION : Utilisez le AudioAttributes natif d'Android
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME) // Important pour spécifier le type d'audio
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    } else {
        null
    }

    // --- LECTURE EN STREAMING ---

    fun playStreamingMusic(url: String, isLooping: Boolean = true) {
        if (isPlaying && url == currentResource) {
            return
        }

        val focusResult = requestAudioFocus()
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("MusicPlayerManager", "La demande de focus audio pour le streaming a échoué.")
            return
        }

        stop()
        currentResource = url

        try {
            mediaPlayer = MediaPlayer().apply {
                // ✅ CORRECTION : On doit aussi passer les AudioAttributes natifs au MediaPlayer
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { player ->
                    player.isLooping = isLooping
                    player.start()
                    Log.d("MusicPlayerManager", "Streaming démarré depuis : $url")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MusicPlayerManager", "Erreur MediaPlayer (streaming): what=$what, extra=$extra")
                    stop()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Erreur lors de la configuration du streaming", e)
            stop()
        }
    }

    // --- CONTRÔLE DE LECTURE ---

    fun stop() {
        if (mediaPlayer == null) return

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: IllegalStateException) {
            Log.w("MusicPlayerManager", "Erreur lors de l'arrêt du MediaPlayer (état invalide): ${e.message}")
        } finally {
            mediaPlayer = null
            currentResource = null
            abandonAudioFocus()
        }
    }

    fun release() {
        stop()
    }

    // --- Fonctions utilitaires pour le focus audio (restent correctes) ---
    private fun requestAudioFocus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }
}
