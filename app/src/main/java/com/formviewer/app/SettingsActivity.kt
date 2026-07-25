package com.formviewer.app

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var urlInput: TextInputEditText
    private lateinit var autoRefreshCheck: CheckBox
    private lateinit var checkRtl: CheckBox
    private lateinit var themeSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSettings)
        toolbar.title = getString(R.string.settings_title)
        toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        urlInput = findViewById(R.id.editFormUrl)
        autoRefreshCheck = findViewById(R.id.checkAutoRefresh)
        checkRtl = findViewById(R.id.checkRtl)
        themeSpinner = findViewById(R.id.spinnerTheme)

        val themeOptions = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        themeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeOptions)

        urlInput.setText(prefs.getString(Constants.KEY_FORM_URL, ""))
        autoRefreshCheck.isChecked = prefs.getBoolean(Constants.KEY_AUTO_REFRESH, false)
        checkRtl.isChecked = prefs.getBoolean(Constants.KEY_RTL_MODE, false)

        val savedTheme = prefs.getString(Constants.KEY_THEME_MODE, "system")
        themeSpinner.setSelection(
            when (savedTheme) {
                "light" -> 1
                "dark" -> 2
                else -> 0
            }
        )

        findViewById<Button>(R.id.btnSave).setOnClickListener { saveSettings() }
        findViewById<Button>(R.id.btnClearCache).setOnClickListener { clearCache() }
    }

    private fun saveSettings() {
        val url = urlInput.text.toString().trim()

        if (url.isBlank() || !Patterns.WEB_URL.matcher(url).matches()) {
            Toast.makeText(this, getString(R.string.enter_valid_url), Toast.LENGTH_SHORT).show()
            return
        }

        val themeValue = when (themeSpinner.selectedItemPosition) {
            1 -> "light"
            2 -> "dark"
            else -> "system"
        }

        prefs.edit()
            .putString(Constants.KEY_FORM_URL, url)
            .putBoolean(Constants.KEY_AUTO_REFRESH, autoRefreshCheck.isChecked)
            .putBoolean(Constants.KEY_RTL_MODE, checkRtl.isChecked)
            .putString(Constants.KEY_THEME_MODE, themeValue)
            .apply()

        applyTheme(themeValue)

        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun applyTheme(themeValue: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (themeValue) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun clearCache() {
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        applicationContext.cacheDir.deleteRecursively()
        Toast.makeText(this, getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
    }
}
