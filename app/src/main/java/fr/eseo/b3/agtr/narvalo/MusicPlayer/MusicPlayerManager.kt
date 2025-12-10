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

    // Create a listener for audio focus changes
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            // Focus lost permanently
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("MusicPlayerManager", "Focus audio perdu définitivement. Arrêt de la musique.")
                mediaPlayer?.stop()
            }
            // Focus lost temporarily
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying) {
                    Log.d("MusicPlayerManager", "Focus audio perdu temporairement. Mise en pause.")
                    mediaPlayer?.pause()
                }
            }
            // Focus lost temporarily but we can continue to play at lower volume
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying) {
                    Log.d("MusicPlayerManager", "Duck: réduction du volume.")
                    mediaPlayer?.setVolume(0.3f, 0.3f)
                }
            }
            //  We recover the focus audio -> we retake the player and the standard volume
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("MusicPlayerManager", "Focus audio regagné. Reprise de la lecture.")
                mediaPlayer?.setVolume(1.0f, 1.0f)
                mediaPlayer?.start()
            }
        }
    }

    //  Create the request for audio focus
    private val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME) // Import specific type of audio
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    } else {
        null
    }


    //  Modify the playLocalMusic method to request focus
    fun playLocalMusic(resId: Int, isLooping: Boolean = true) {
        if (isPlaying && resId == currentResId) {
            return
        }

        // ---  Request the focus audio ---
        val focusResult = requestAudioFocus()
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("MusicPlayerManager", "La demande de focus audio a échoué.")
            return
        }
        // --- End of the focus request ---

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


    // 5.  Modify the stop() method to abandon focus
    fun stop() {
        if (mediaPlayer == null) return

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentResId = null

        // --- Abandon of focus audio---
        abandonAudioFocus()
    }

    fun release() {
        stop() // stop() abandon when the focus
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
