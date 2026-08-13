package com.finuchyot.tbank

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.net.URI

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var address: TextView
    private lateinit var progress: ProgressBar
    private var desktopModeEnabled = false

    companion object {
        private val TRUSTED_SUFFIXES = setOf("tbank.ru", "tinkoff.ru")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 8)
        }
        val warning = TextView(this).apply {
            text = getString(R.string.security_notice)
            setTextColor(0xff1a1a1a.toInt())
            setBackgroundColor(0xffffe600.toInt())
            setPadding(18, 14, 18, 14)
        }
        address = TextView(this).apply {
            text = "Официальный сайт ещё не открыт"
            setPadding(4, 12, 4, 8)
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val open = Button(this).apply {
            text = "Мобильный вид"
            setOnClickListener { confirmAndOpenBank(BrowserMode.mobile(webView.settings.userAgentString)) }
        }
        val desktop = Button(this).apply {
            text = "Операции (ПК)"
            setOnClickListener { confirmAndOpenBank(BrowserMode.desktop()) }
        }
        val reports = Button(this).apply {
            text = "Отчёты"
            setOnClickListener { startActivity(Intent(this@MainActivity, FilesActivity::class.java)) }
        }
        controls.addView(open, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(desktop, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(reports, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        webView = WebView(this)
        configureWebView()

        root.addView(warning)
        root.addView(address)
        root.addView(progress)
        root.addView(controls)
        root.addView(webView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun configureWebView() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, false)
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            mediaPlaybackRequiresUserGesture = true
        }
        WebView.setWebContentsDebuggingEnabled(false)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 0..99) View.VISIBLE else View.GONE
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (!request.isForMainFrame) return false
                return if (isTrusted(uri)) {
                    address.text = "Официальный домен: ${uri.host}"
                    false
                } else {
                    showBlocked(uri)
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let {
                    val uri = Uri.parse(it)
                    address.text = if (isTrusted(uri))
                        "Официальный домен: ${uri.host}"
                    else "Переход заблокирован"
                    if (desktopModeEnabled && isTrusted(uri)) {
                        view?.evaluateJavascript(DesktopViewportScript.build(1280)) { raw ->
                            address.text = "Официальный домен: ${uri.host} · viewport ${DesktopViewportScript.parseMetrics(raw)}"
                        }
                    }
                }
            }
        }
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            handleDownload(url, userAgent, contentDisposition, mimeType)
        })
    }

    private fun confirmAndOpenBank(config: BrowserConfig) {
        val modeName = if (config.useWideViewPort) "десктопный раздел всех операций" else "мобильный сайт"
        AlertDialog.Builder(this)
            .setTitle("Открыть $modeName?")
            .setMessage("Проверьте домен в жёлтой панели. Номер телефона, одноразовый код и код быстрого входа вводятся только на странице банка. Приложение их не записывает.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Открыть") { _, _ ->
                applyBrowserMode(config)
                webView.loadUrl(config.startUrl)
            }
            .show()
    }

    private fun applyBrowserMode(config: BrowserConfig) {
        desktopModeEnabled = config.useWideViewPort
        webView.settings.apply {
            userAgentString = config.userAgent
            useWideViewPort = config.useWideViewPort
            loadWithOverviewMode = config.loadWithOverviewMode
            textZoom = if (config.useWideViewPort) 80 else 100
        }
        webView.clearCache(false)
    }

    private fun handleDownload(url: String, userAgent: String?, disposition: String?, mime: String?) {
        val uri = Uri.parse(url)
        if (!isTrusted(uri)) {
            showBlocked(uri)
            return
        }
        if (uri.scheme == "blob") {
            Toast.makeText(this, "Этот тип экспорта WebView не поддержал. Выберите файловый формат или откройте экспорт в обычном браузере.", Toast.LENGTH_LONG).show()
            return
        }
        val filename = safeFilename(URLUtil.guessFileName(url, disposition, mime))
        val request = DownloadManager.Request(uri).apply {
            setTitle(filename)
            setDescription("Выгрузка с официального сайта Т‑Банка")
            setMimeType(mime)
            userAgent?.let { addRequestHeader("User-Agent", it) }
            CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOCUMENTS, "reports/$filename")
        }
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(this, "Загрузка начата: $filename", Toast.LENGTH_LONG).show()
    }

    private fun safeFilename(name: String): String {
        val clean = name.replace(Regex("[^A-Za-zА-Яа-я0-9._ -]"), "_").take(120)
        return clean.ifBlank { "tbank-report-${System.currentTimeMillis()}.bin" }
    }

    private fun isTrusted(uri: Uri): Boolean {
        if (uri.scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return TRUSTED_SUFFIXES.any { host == it || host.endsWith(".$it") }
    }

    private fun showBlocked(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Переход заблокирован")
            .setMessage("Приложение разрешает основной странице открываться только на HTTPS-доменах Т‑Банка. Адрес: ${uri.host ?: "неизвестен"}")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }
}
