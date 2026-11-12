package com.template.webastra

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read features from assets/config.json
        val configStr = assets.open("config.json").bufferedReader().use { it.readText() }
        val config = JSONObject(configStr)

        val pullToRefresh = config.optBoolean("pullToRefresh", true)
        val loaderScreen = config.optBoolean("loaderScreen", true)
        val fullscreenMode = config.optBoolean("fullscreenMode", false)
        val navigationButtons = config.optBoolean("navigationButtons", false)
        val externalLinks = config.optBoolean("externalLinksInBrowser", true)

        val targetUrl = getString(R.string.target_url)
        val themeColor = ContextCompat.getColor(this, R.color.primary_theme)

        if (fullscreenMode) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.statusBarColor = themeColor
        }

        // Root Layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Progress Bar (Loader)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 8)
            max = 100
            visibility = if (loaderScreen) View.VISIBLE else View.GONE
        }
        rootLayout.addView(progressBar)

        // WebView Setup
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
        }

        val targetHost = Uri.parse(targetUrl).host ?: ""

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                if (externalLinks && url.host != null && !url.host!!.contains(targetHost)) {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (loaderScreen) progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (loaderScreen) progressBar.progress = newProgress
            }
        }

        // SwipeRefreshLayout
        swipeRefresh = SwipeRefreshLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isEnabled = pullToRefresh
            setColorSchemeColors(themeColor)
            setOnRefreshListener { webView.reload() }
            addView(webView)
        }
        rootLayout.addView(swipeRefresh)

        // Optional Bottom Navigation Buttons
        if (navigationButtons) {
            val navBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#F8F9FA"))
                setPadding(16, 12, 16, 12)
                gravity = Gravity.CENTER
            }

            fun createNavBtn(label: String, action: () -> Unit): TextView {
                return TextView(this).apply {
                    text = label
                    textSize = 16f
                    setTextColor(themeColor)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { action() }
                }
            }

            navBar.addView(createNavBtn("◀ Back") { if (webView.canGoBack()) webView.goBack() })
            navBar.addView(createNavBtn("Forward ▶") { if (webView.canGoForward()) webView.goForward() })
            navBar.addView(createNavBtn("↻ Refresh") { webView.reload() })
            rootLayout.addView(navBar)
        }

        setContentView(rootLayout)
        webView.loadUrl(targetUrl)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }
}
