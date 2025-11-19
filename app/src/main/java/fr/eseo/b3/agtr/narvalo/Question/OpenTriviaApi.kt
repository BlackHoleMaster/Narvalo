package fr.eseo.b3.agtr.narvalo.Question

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenTriviaApi {
    @GET("api.php")
    suspend fun getQuestions(
        @Query("amount") amount: Int = 10,
        @Query("difficulty") difficulty: String? = null,
        @Query("type") type: String = "multiple"
    ): TriviaResponse

    companion object {
        const val BASE_URL = "https://opentdb.com/"
    }
}

