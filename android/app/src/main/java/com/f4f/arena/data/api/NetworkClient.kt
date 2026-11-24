package com.f4f.arena.data.api

import com.f4f.arena.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Configurable base URL: Defaults to 10.0.2.2:8080/ on emulator, or custom IP set in local.properties
    val baseUrl: String = BuildConfig.API_BASE_URL

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val healthApi: HealthApi by lazy {
        retrofit.create(HealthApi::class.java)
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val playerApi: PlayerApi by lazy {
        retrofit.create(PlayerApi::class.java)
    }

    val matchApi: MatchApi by lazy {
        retrofit.create(MatchApi::class.java)
    }

    val teamApi: TeamApi by lazy {
        retrofit.create(TeamApi::class.java)
    }
}
