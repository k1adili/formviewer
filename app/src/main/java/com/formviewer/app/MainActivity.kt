package com.formviewer.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: TextView
    private lateinit var prefs: SharedPreferences

    private var lastLoadedUrl: String? = null

    // ---- Force the whole app's locale to Persian BEFORE the Activity/WebView is created ----
    // This makes the WebView send "fa" as its default Accept-Language on every single
    // network request it makes internally (including the Google login redirects),
    // instead of us trying to inject a header manually on just the first load.
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val rtl = prefs.getBoolean(Constants.KEY_RTL_MODE, false)

        if (rtl) {
            val locale = Locale("fa", "IR")
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        emptyStateView = findViewById(R.id.emptyStateView)

        setupWebView()

        swipeRefresh.setOnRefreshListener {
            if (isNetworkAvailable()) {
                webView.reload()
            } else {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }

        emptyStateView.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        // NOTE: no custom shouldOverrideUrlLoading / manual reload-with-headers here anymore.
        // Manually re-loading the URL mid-navigation was interfering with the Google login flow.
        // Language is now handled once, globally, via attachBaseContext() above.
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                if (request?.isForMainFrame == true) {
                    showEmptyState(getString(R.string.load_error))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the RTL setting was changed in the Settings screen while this Activity
        // was already alive, attachBaseContext() won't re-run on its own.
        // Detect the mismatch and recreate the Activity once so the new locale applies.
        if (localeNeedsRecreate()) {
            recreate()
            return
        }
        loadFormIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    private fun localeNeedsRecreate(): Boolean {
        val rtl = prefs.getBoolean(Constants.KEY_RTL_MODE, false)
        val currentLang = resources.configuration.locales[0].language
        return (rtl && currentLang != "fa") || (!rtl && currentLang == "fa")
    }

    private fun loadFormIfNeeded() {
        val url = prefs.getString(Constants.KEY_FORM_URL, null)
        val autoRefresh = prefs.getBoolean(Constants.KEY_AUTO_REFRESH, false)

        if (url.isNullOrBlank()) {
            showEmptyState(getString(R.string.no_url_set))
            return
        }

        if (!isNetworkAvailable()) {
            showEmptyState(getString(R.string.no_internet))
            return
        }

        hideEmptyState()

        if (autoRefresh || url != lastLoadedUrl) {
            val rtl = prefs.getBoolean(Constants.KEY_RTL_MODE, false)
            val finalUrl = if (rtl) {
                if (url.contains("?")) "$url&hl=fa" else "$url?hl=fa"
            } else {
                url
            }
            webView.loadUrl(finalUrl)
            lastLoadedUrl = url
        }
    }

    private fun showEmptyState(message: String) {
        webView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyStateView.text = message
    }

    private fun hideEmptyState() {
        webView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                if (isNetworkAvailable()) webView.reload() else loadFormIfNeeded()
                true
            }
            R.id.action_open_browser -> {
                val url = prefs.getString(Constants.KEY_FORM_URL, null)
                if (!url.isNullOrBlank()) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } else {
                    Toast.makeText(this, getString(R.string.no_url_set), Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
