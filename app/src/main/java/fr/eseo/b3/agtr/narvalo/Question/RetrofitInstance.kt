package fr.eseo.b3.agtr.narvalo.Question

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(OpenTriviaApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: OpenTriviaApi by lazy {
        retrofit.create(OpenTriviaApi::class.java)
    }
}

