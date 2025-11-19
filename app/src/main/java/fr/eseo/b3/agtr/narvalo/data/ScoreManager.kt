package fr.eseo.b3.agtr.narvalo.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ScoreData(
    @SerializedName("high_score")
    val highScore: Int = 0,
    @SerializedName("last_score")
    val lastScore: Int = 0
)

class ScoreManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("narvalo_scores", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_SCORES = "scores_data"
    }

    /**
     * Sauvegarde les scores dans un fichier JSON
     */
    fun saveScores(highScore: Int, lastScore: Int) {
        val scoreData = ScoreData(highScore, lastScore)
        val json = gson.toJson(scoreData)
        prefs.edit().putString(KEY_SCORES, json).apply()
    }

    /**
     * Récupère les scores depuis le fichier JSON
     */
    fun loadScores(): ScoreData {
        val json = prefs.getString(KEY_SCORES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ScoreData::class.java)
            } catch (e: Exception) {
                ScoreData() // Retourne des scores par défaut en cas d'erreur
            }
        } else {
            ScoreData() // Première utilisation
        }
    }

    /**
     * Met à jour uniquement le dernier score
     */
    fun updateLastScore(score: Int) {
        val currentScores = loadScores()
        val newHighScore = maxOf(currentScores.highScore, score)
        saveScores(newHighScore, score)
    }

    /**
     * Récupère uniquement le high score
     */
    fun getHighScore(): Int {
        return loadScores().highScore
    }

    /**
     * Récupère uniquement le last score
     */
    fun getLastScore(): Int {
        return loadScores().lastScore
    }

    /**
     * Réinitialise tous les scores
     */
    fun resetScores() {
        saveScores(0, 0)
    }
}

