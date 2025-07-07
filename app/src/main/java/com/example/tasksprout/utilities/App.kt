package com.example.tasksprout.utilities

import android.app.Application
import com.example.tasksprout.CurrentActivityProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SignalManager.init(this)
        ImageLoader.init(this)
        registerActivityLifecycleCallbacks(CurrentActivityProvider)
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
