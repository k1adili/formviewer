package com.formviewer.app

import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.Gravity
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var urlInput: TextInputEditText
    private lateinit var autoRefreshCheck: CheckBox
    private lateinit var rtlCheck: CheckBox
    private lateinit var themeSpinner: Spinner
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSettings)
        toolbar.title = getString(R.string.settings_title)
        toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        urlInput = findViewById(R.id.editFormUrl)
        autoRefreshCheck = findViewById(R.id.checkAutoRefresh)
        rtlCheck = findViewById(R.id.checkRtl)
        themeSpinner = findViewById(R.id.spinnerTheme)
        historyContainer = findViewById(R.id.historyContainer)

        val themeOptions = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        themeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeOptions)

        urlInput.setText(prefs.getString(Constants.KEY_FORM_URL, ""))
        autoRefreshCheck.isChecked = prefs.getBoolean(Constants.KEY_AUTO_REFRESH, false)
        rtlCheck.isChecked = prefs.getBoolean(Constants.KEY_RTL_MODE, false)

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

        renderHistory()
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
            .putBoolean(Constants.KEY_RTL_MODE, rtlCheck.isChecked)
            .putString(Constants.KEY_THEME_MODE, themeValue)
            .apply()

        addToHistory(url)
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

    // ---------- Recent-forms history ----------

    private fun loadHistory(): List<String> {
        val raw = prefs.getString(Constants.KEY_FORM_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(Constants.KEY_FORM_HISTORY, arr.toString()).apply()
    }

    private fun addToHistory(url: String) {
        val current = loadHistory().toMutableList()
        current.remove(url)
        current.add(0, url)
        saveHistory(current.take(10))
        renderHistory()
    }

    private fun removeFromHistory(url: String) {
        val current = loadHistory().toMutableList()
        current.remove(url)
        saveHistory(current)
        renderHistory()
    }

    private fun renderHistory() {
        historyContainer.removeAllViews()
        val history = loadHistory()

        if (history.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = getString(R.string.history_empty)
            emptyText.setPadding(0, 12, 0, 0)
            historyContainer.addView(emptyText)
            return
        }

        for (item in history) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val rowParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            rowParams.topMargin = 8
            row.layoutParams = rowParams

            val urlText = TextView(this)
            urlText.text = item
            urlText.maxLines = 1
            urlText.ellipsize = TextUtils.TruncateAt.MIDDLE
            urlText.setPadding(12, 12, 12, 12)
            val urlParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            urlText.layoutParams = urlParams
            urlText.setOnClickListener {
                urlInput.setText(item)
            }

            val deleteBtn = TextView(this)
            deleteBtn.text = getString(R.string.delete)
            deleteBtn.setPadding(16, 12, 12, 12)
            deleteBtn.setTextColor(resources.getColor(R.color.colorAccent, theme))
            deleteBtn.setOnClickListener {
                removeFromHistory(item)
            }

            row.addView(urlText)
            row.addView(deleteBtn)
            historyContainer.addView(row)
        }
    }
}
