package com.example.tasksprout

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

object CurrentActivityProvider : Application.ActivityLifecycleCallbacks {

    private var currentActivityRef: WeakReference<Activity>? = null

    fun getActivity(): Activity? = currentActivityRef?.get()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        val current = currentActivityRef?.get()
        if (current == activity) {
            currentActivityRef = null
        }
    }
}
