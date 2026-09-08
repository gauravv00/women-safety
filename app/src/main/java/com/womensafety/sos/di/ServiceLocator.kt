package com.womensafety.sos.di

import android.content.Context
import com.womensafety.sos.data.local.AppDatabase
import com.womensafety.sos.data.pref.UserPreferencesRepository
import com.womensafety.sos.data.repository.SafetyRepositoryImpl
import com.womensafety.sos.domain.repository.SafetyRepository

object ServiceLocator {
    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var _repository: SafetyRepository? = null

    @Volatile
    private var _userPreferences: UserPreferencesRepository? = null

    val repository: SafetyRepository
        get() = _repository ?: throw IllegalStateException("ServiceLocator not initialized. Call init(context) first.")

    val userPreferences: UserPreferencesRepository
        get() = _userPreferences ?: throw IllegalStateException("ServiceLocator not initialized. Call init(context) first.")

    fun init(context: Context) {
        if (_repository == null || _userPreferences == null) {
            synchronized(this) {
                val db = database ?: AppDatabase.getDatabase(context).also { database = it }
                val prefs = _userPreferences ?: UserPreferencesRepository(context.applicationContext).also { _userPreferences = it }
                if (_repository == null) {
                    _repository = SafetyRepositoryImpl(
                        context = context.applicationContext,
                        contactDao = db.trustedContactDao(),
                        incidentDao = db.incidentLogDao()
                    )
                }
            }
        }
    }
}
