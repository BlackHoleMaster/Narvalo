// Fichier: MusicPlayer/MusicPlayerManager.kt
package fr.eseo.b3.agtr.narvalo.MusicPlayer

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class MusicPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false


    private var currentResId: Int? = null


    fun playLocalMusic(resId: Int, isLooping: Boolean = true) {
        // Si la bonne musique est déjà en cours de lecture, ne rien faire
        if (isPlaying && resId == currentResId) {
            return
        }

        stop() // Arrêter toute musique précédente
        currentResId = resId // Mémoriser le nouvel identifiant

        try {
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.isLooping = isLooping
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Erreur lors de la création du MediaPlayer local", e)
            stop() // Nettoyer en cas d'erreur
        }
    }

    // La méthode pour le streaming reste disponible si besoin
    fun playStreamingMusic(url: Int, isLooping: Boolean = true) {
        // ... (pas besoin de modifier cette méthode)
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentResId = null
    }

    fun release() {
        stop()
    }
}
