package com.example.cemeterylocator.api

import android.content.Context
import com.example.cemeterylocator.model.User

object SessionManager {

    private const val PREFS_NAME = "cemetery_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_full_name"

    fun saveSession(context: Context, token: String, user: User) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, user.userId)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.fullName)
            .apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    fun getUserName(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_NAME, null)

    fun getUserEmail(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_EMAIL, null)

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
