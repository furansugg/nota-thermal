package com.notathermal.app

import android.app.Application
import com.notathermal.app.di.AppContainer

class NotaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
