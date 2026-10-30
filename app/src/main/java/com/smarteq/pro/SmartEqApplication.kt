package com.smarteq.pro

import android.app.Application
import com.smarteq.pro.di.AppContainer
import com.smarteq.pro.di.DefaultAppContainer

class SmartEqApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
