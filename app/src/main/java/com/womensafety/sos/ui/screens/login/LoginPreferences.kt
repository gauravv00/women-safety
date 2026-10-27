package com.womensafety.sos.ui.screens.login

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object LoginPreferences {
    private const val PREFS_NAME = "login_prefs"
    private const val KEY_LOGIN_COMPLETED = "login_completed"

    fun isLoginCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loginCompleted = prefs.getBoolean(KEY_LOGIN_COMPLETED, false)
        val user = FirebaseAuth.getInstance().currentUser
        return loginCompleted && user != null && !user.isAnonymous
    }

    fun setLoginCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOGIN_COMPLETED, completed).apply()
    }
}
