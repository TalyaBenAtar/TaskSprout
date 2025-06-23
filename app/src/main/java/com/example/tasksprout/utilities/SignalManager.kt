package com.example.tasksprout.utilities

import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.annotation.RequiresPermission
import java.lang.ref.WeakReference
import kotlin.also
import kotlin.let

class SignalManager private constructor(context: Context) {
    private val contextRef = WeakReference(context)
    private var currentToast: Toast? = null

    companion object {
        @Volatile
        private var instance: SignalManager? = null

        fun init(context: Context): SignalManager {
            return instance ?: synchronized(this) {
                instance ?: SignalManager(context).also { instance = it }
            }
        }

        fun getInstance(): SignalManager {
            return instance ?: throw IllegalStateException(
                "SignalManager must be initialized by calling init(context) before use."
            )
        }
    }

    fun toast(text: String) {
        contextRef.get()?.let { context: Context ->
            currentToast?.cancel()
            currentToast = Toast.makeText(context, text, Toast.LENGTH_SHORT)
            currentToast?.show()
        }
    }

    fun vibrate() {

        contextRef.get()?.let { context: Context ->
            val vibrator: Vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager =
                        context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                val SOSPattern = longArrayOf(
                    0,
                    200,
                    100,
                    200,
                    100,
                    200,
                    300,
                    500,
                    100,
                    500,
                    100,
                    500,
                    300,
                    200,
                    100,
                    200,
                    100,
                    200
                )

                val waveFormVibrationEffect =
                    VibrationEffect.createWaveform(
                        SOSPattern,
                        -1
                    )

                val oneShotVibrationEffect =
                    VibrationEffect.createOneShot(
                        500,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )

                vibrator.vibrate(oneShotVibrationEffect)
 //               vibrator.vibrate(waveFormVibrationEffect)
            }else{
                vibrator.vibrate(500)
            }
        }
    }
}