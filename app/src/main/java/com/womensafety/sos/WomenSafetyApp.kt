package com.womensafety.sos

import android.app.Application
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WomenSafetyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        ServiceLocator.init(this)
        // Eagerly authenticate and sync user document so pairing code is in Firestore
        // before the user ever opens Settings. Runs on IO thread; failures are logged silently.
        CoroutineScope(Dispatchers.IO).launch {
            ServiceLocator.authManager.ensureAuthenticated()
        }
    }
}
