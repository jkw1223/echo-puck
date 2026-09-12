package com.steve.puckd

import android.content.Context

object RealtimeVoicePreferences {
    const val KEY = "realtime_voice"
    const val DEFAULT = "cedar"
    val SUPPORTED = listOf("alloy", "ash", "ballad", "coral", "echo", "sage", "shimmer", "verse", "marin", "cedar")

    fun get(context: Context): String {
        val value = context.getSharedPreferences("puck_status", Context.MODE_PRIVATE).getString(KEY, DEFAULT)
        return if (value in SUPPORTED) value!! else DEFAULT
    }

    fun set(context: Context, voice: String) {
        if (voice in SUPPORTED) context.getSharedPreferences("puck_status", Context.MODE_PRIVATE).edit().putString(KEY, voice).apply()
    }
}
