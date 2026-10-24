package com.womensafety.sos

import android.app.Application
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.service.NotificationHelper

class WomenSafetyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        ServiceLocator.init(this)
    }
}
