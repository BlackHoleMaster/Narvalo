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

    // Save scores to JSON file
    fun saveScores(highScore: Int, lastScore: Int) {
        val scoreData = ScoreData(highScore, lastScore)
        val json = gson.toJson(scoreData)
        prefs.edit().putString(KEY_SCORES, json).apply()
    }

    // Get scores from JSON file
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

    // Update only the last score and adjust high score if needed
    fun updateLastScore(score: Int) {
        val currentScores = loadScores()
        val newHighScore = maxOf(currentScores.highScore, score)
        saveScores(newHighScore, score)
    }

    // Get only the high score
    fun getHighScore(): Int {
        return loadScores().highScore
    }

    // Get only the last score
    fun getLastScore(): Int {
        return loadScores().lastScore
    }

    // Reset scores to zero
    fun resetScores() {
        saveScores(0, 0)
    }
}

