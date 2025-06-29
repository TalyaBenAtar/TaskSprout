package com.example.tasksprout.utilities

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SignalManager.init(this)
        ImageLoader.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
    }

     fun onPause() {
        super.onTerminate()
    }

     fun onResume() {
        super.onTerminate()
    }
}
