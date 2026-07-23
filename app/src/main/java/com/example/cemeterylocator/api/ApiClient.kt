package com.example.cemeterylocator.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds a fresh Retrofit client against whatever base URL is currently
 * saved in preferences, since this app points at a self-hosted PHP
 * backend whose address varies per deployment (see SettingsActivity).
 */
object ApiClient {

    private const val PREFS_NAME = "cemetery_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val DEFAULT_BASE_URL = "http://10.0.2.2/" // localhost, from the Android emulator's perspective

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()
    }

    fun create(context: Context): CemeteryApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val token = SessionManager.getToken(context)
            val request = if (token != null) {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(getBaseUrl(context))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CemeteryApi::class.java)
    }
}
