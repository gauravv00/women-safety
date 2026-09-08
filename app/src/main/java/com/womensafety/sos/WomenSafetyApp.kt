package com.womensafety.sos

import android.app.Application
import com.womensafety.sos.di.ServiceLocator

class WomenSafetyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
