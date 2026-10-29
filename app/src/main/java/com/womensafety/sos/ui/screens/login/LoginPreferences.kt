package com.womensafety.sos.ui.screens.login

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object LoginPreferences {
    private const val PREFS_NAME = "login_prefs"
    private const val KEY_LOGIN_COMPLETED = "login_completed"
    private const val KEY_GUEST_SKIPPED = "guest_skipped"

    fun isLoginCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loginCompleted = prefs.getBoolean(KEY_LOGIN_COMPLETED, false)
        val guestSkipped = prefs.getBoolean(KEY_GUEST_SKIPPED, false)
        val user = FirebaseAuth.getInstance().currentUser
        return (loginCompleted && user != null && !user.isAnonymous) || guestSkipped
    }

    fun setLoginCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOGIN_COMPLETED, completed).apply()
    }

    fun setLoginSkipped(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GUEST_SKIPPED, true).apply()
    }

    fun clearLogin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
