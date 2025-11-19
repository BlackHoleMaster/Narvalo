// Fichier: MusicPlayerManager.kt
package fr.eseo.b3.agtr.narvalo.MusicPlayer// ✅ Assurez-vous que ce package est correct

import android.content.Context
import android.media.MediaPlayer

class MusicPlayerManager(private val context: Context) {
    // ... (le code reste identique à celui de la réponse précédente) ...

    private var mediaPlayer: MediaPlayer? = null
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    fun playLocalMusic(resId: Int, isLooping: Boolean = true) {
        if (isPlaying) return
        stop()
        mediaPlayer = MediaPlayer.create(context, resId)
        mediaPlayer?.isLooping = isLooping
        mediaPlayer?.start()
    }

    fun stop() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
