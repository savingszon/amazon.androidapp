package com.savingszon.application

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import android.net.Uri
import android.webkit.WebResourceRequest

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Nothing here — WebView stays intact
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        //webView.webViewClient = WebViewClient()
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d("WebView", "Loading URL: $url")

                return if (url.contains("https://www.amazon") && !url.contains("https://www.savingszon")) {
                    // Open with external app (Amazon app or browser)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true // Don't load in WebView
                } else {
                    false // Load in WebView
                }
            }
        }

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (!sharedText.isNullOrEmpty()) {
            // Step 0: Get the short URL
            // done in step 1.

            // Step 1: Resolve the short URL in background
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("WebView", sharedText)
                val url = extractUrls(sharedText)
                if (url.isEmpty()) {
                    webView.loadUrl("https://www.savingszon.com")
                } else {
                    val finalUrl = getFinalUrlWithOkHttp(url.get(0)) // https://amzn.to/44EhUih
                    val cleanedUrl = cleanAmazonUrl(finalUrl)
                    Log.d("WebView", cleanedUrl)
                    val redirectUrl = "https://www.savingszon.com/?q=" + Uri.encode(cleanedUrl)
                    Log.d("WebView", "Loading resolved URL: $redirectUrl")

                    // Step 2: Load into WebView on main thread
                    withContext(Dispatchers.Main) {
                        webView.loadUrl(redirectUrl)
                    }
                }
            }
        } else {
            // No shared link, load default homepage
            webView.loadUrl("https://www.savingszon.com/?c=androidapp_v105")
        }
    }

    // OkHttp redirect resolver
    private fun getFinalUrlWithOkHttp(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            return response.request.url.toString()
        }
    }

    fun extractUrls(text: String): List<String> {
        // simple URL regex (http/https). It captures until whitespace.
        val urlRegex = "(https?://[^\\s]+)".toRegex(RegexOption.IGNORE_CASE)
        return urlRegex.findAll(text)
            .map { it.value.trim().trimEnd('.', ',', ';', ')', ']', '\"', '\'') } // remove common trailing punctuation
            .distinct()
            .toList()
    }

    fun cleanAmazonUrl(url: String): String {
        val regex = "(https?://www\\.amazon\\.[a-z.]+/dp/[A-Z0-9]{10})".toRegex()
        val match = regex.find(url)
        return match?.value ?: url // return cleaned if matched, else original
    }
}
