// Fichier: MusicPlayer/MusicPlayerManager.kt
package fr.eseo.b3.agtr.narvalo.MusicPlayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log

class MusicPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    private var currentResId: Int? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // ✅ 2. Créer un listener pour les changements de focus audio
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            // Focus perdu de manière permanente (ex: une autre app de musique se lance)
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("MusicPlayerManager", "Focus audio perdu définitivement. Arrêt de la musique.")
                // On ne peut pas appeler stop() directement ici si QuizScreen doit mettre à jour son UI.
                // Pour l'instant, on arrête la lecture.
                mediaPlayer?.stop()
            }
            // Focus perdu temporairement (ex: appel entrant) -> on met en pause
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying) {
                    Log.d("MusicPlayerManager", "Focus audio perdu temporairement. Mise en pause.")
                    mediaPlayer?.pause()
                }
            }
            // Focus perdu temporairement, mais on peut continuer à jouer à bas volume (ex: notification)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying) {
                    Log.d("MusicPlayerManager", "Duck: réduction du volume.")
                    mediaPlayer?.setVolume(0.3f, 0.3f)
                }
            }
            // On a (re)gagné le focus audio -> on reprend la lecture et on remet le volume normal
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("MusicPlayerManager", "Focus audio regagné. Reprise de la lecture.")
                mediaPlayer?.setVolume(1.0f, 1.0f)
                mediaPlayer?.start()
            }
        }
    }

    // ✅ 3. Créer la requête de focus audio (pour Android 8.0+ et versions ultérieures)
    private val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
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


    // ✅ 4. Modifier la méthode playLocalMusic pour demander le focus
    fun playLocalMusic(resId: Int, isLooping: Boolean = true) {
        if (isPlaying && resId == currentResId) {
            return
        }

        // --- Demande de focus audio ---
        val focusResult = requestAudioFocus()
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("MusicPlayerManager", "La demande de focus audio a échoué.")
            return // Si on n'a pas le focus, on ne joue pas la musique
        }
        // --- Fin de la demande de focus ---

        stop()
        currentResId = resId

        try {
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.isLooping = isLooping
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Erreur lors de la création du MediaPlayer local", e)
            stop()
        }
    }

    // La méthode pour le streaming reste disponible si besoin
    fun playStreamingMusic(url: Int, isLooping: Boolean = true) {
        // ... (pas besoin de modifier cette méthode)
    }

    // ✅ 5. Modifier la méthode stop() pour abandonner le focus
    fun stop() {
        if (mediaPlayer == null) return

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentResId = null

        // --- Abandon du focus audio ---
        abandonAudioFocus()
    }

    fun release() {
        stop() // stop() abandonne déjà le focus
    }

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
