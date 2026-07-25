package com.formviewer.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class FormViewerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val theme = prefs.getString(Constants.KEY_THEME_MODE, "system")
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
