package com.womensafety.sos.di

import android.content.Context
import com.womensafety.sos.data.firebase.FirebaseAuthManager
import com.womensafety.sos.data.local.AppDatabase
import com.womensafety.sos.data.location.LocationTracker
import com.womensafety.sos.data.pref.UserPreferencesRepository
import com.womensafety.sos.data.repository.NotificationRepositoryImpl
import com.womensafety.sos.data.repository.SafetyRepositoryImpl
import com.womensafety.sos.domain.repository.NotificationRepository
import com.womensafety.sos.domain.repository.SafetyRepository

object ServiceLocator {
    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var _repository: SafetyRepository? = null

    @Volatile
    private var _userPreferences: UserPreferencesRepository? = null

    @Volatile
    private var _locationTracker: LocationTracker? = null

    @Volatile
    private var _authManager: FirebaseAuthManager? = null

    @Volatile
    private var _notificationRepository: NotificationRepository? = null

    val repository: SafetyRepository
        get() = _repository ?: throw IllegalStateException("ServiceLocator not initialized. Call init(context) first.")

    val userPreferences: UserPreferencesRepository
        get() = _userPreferences ?: throw IllegalStateException("ServiceLocator not initialized. Call init(context) first.")

    val locationTracker: LocationTracker
        get() = _locationTracker ?: throw IllegalStateException("ServiceLocator not initialized. Call init(context) first.")

    val authManager: FirebaseAuthManager
        get() = _authManager ?: throw IllegalStateException("ServiceLocator not initialized. Call init(context) first.")

    val notificationRepository: NotificationRepository
        get() = _notificationRepository ?: throw IllegalStateException("ServiceLocator not initialized. Call init(context) first.")

    fun init(context: Context) {
        if (_repository == null || _userPreferences == null || _locationTracker == null || _authManager == null || _notificationRepository == null) {
            synchronized(this) {
                val db = database ?: AppDatabase.getDatabase(context).also { database = it }
                if (_userPreferences == null) {
                    _userPreferences = UserPreferencesRepository(context.applicationContext)
                }
                if (_locationTracker == null) {
                    _locationTracker = LocationTracker(context.applicationContext)
                }
                if (_authManager == null) {
                    _authManager = FirebaseAuthManager.getInstance(context.applicationContext)
                }
                if (_notificationRepository == null) {
                    _notificationRepository = NotificationRepositoryImpl(_authManager!!)
                }
                if (_repository == null) {
                    _repository = SafetyRepositoryImpl(
                        context = context.applicationContext,
                        contactDao = db.trustedContactDao(),
                        incidentDao = db.incidentLogDao(),
                        pairedWardDao = db.pairedWardDao()
                    )
                }
            }
        }
    }
}
