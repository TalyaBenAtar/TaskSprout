package com.example.tasksprout.utilities

import kotlin.text.format


object TimeFormatter {
    fun formatTime(delta: Long): String {
        var seconds = (delta / 1000).toInt()
        var minutes = seconds / 60
        seconds %= 60
        var hours = minutes / 60
        minutes %= 60
        hours %= 24

        return buildString {
            append(String.format(locale = null, "%02d", hours))
            append(":")
            append(String.format(locale = null, "%02d", minutes))
            append(":")
            append(String.format(locale = null, "%02d", seconds))
        }
    }
}