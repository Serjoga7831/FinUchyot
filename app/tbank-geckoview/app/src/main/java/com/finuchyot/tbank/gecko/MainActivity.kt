package com.finuchyot.tbank.gecko

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

class MainActivity : AppCompatActivity() {
    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession
    private lateinit var runtime: GeckoRuntime
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private var canGoBack = false

    companion object {
        private const val OPERATIONS_URL = "https://www.tbank.ru/mybank/operations/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 8)
        }
        val notice = TextView(this).apply {
            text = "Независимый движок GeckoView в официальном desktop mode. Вводите номер и код только на домене tbank.ru."
            setTextColor(0xff111111.toInt())
            setBackgroundColor(0xffffe600.toInt())
            setPadding(16, 12, 16, 12)
        }
        status = TextView(this).apply {
            text = "Страница ещё не открыта"
            setPadding(4, 10, 4, 8)
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            visibility = View.GONE
        }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(Button(this).apply {
            text = "Операции (ПК)"
            setOnClickListener { confirmOpen() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(Button(this).apply {
            text = "Обновить"
            setOnClickListener { session.reload() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        geckoView = GeckoView(this)
        root.addView(notice)
        root.addView(status)
        root.addView(progress)
        root.addView(controls)
        root.addView(geckoView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)

        initializeGecko()
    }

    private fun initializeGecko() {
        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .remoteDebuggingEnabled(false)
            .consoleOutput(false)
            .screenSizeOverride(1280, 900)
            .loginAutofillEnabled(false)
            .build()
        runtime = GeckoRuntime.create(applicationContext, runtimeSettings)

        val sessionSettings = GeckoSessionSettings.Builder()
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
            .build()
        session = GeckoSession(sessionSettings)
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny> {
                if (request.target != GeckoSession.NavigationDelegate.TARGET_WINDOW_CURRENT) {
                    return GeckoResult.deny()
                }
                return if (TrustedBankUrl.isAllowed(request.uri)) {
                    status.text = "Официальный адрес: ${java.net.URI(request.uri).host}"
                    GeckoResult.allow()
                } else {
                    runOnUiThread { showBlocked(request.uri) }
                    GeckoResult.deny()
                }
            }

            override fun onCanGoBack(session: GeckoSession, value: Boolean) {
                canGoBack = value
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                status.text = if (url != null && TrustedBankUrl.isAllowed(url))
                    "Официальный адрес: $url"
                else "Переход заблокирован"
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                progress.visibility = View.VISIBLE
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                progress.visibility = View.GONE
                if (!success) Toast.makeText(this@MainActivity, "Страница не загрузилась", Toast.LENGTH_SHORT).show()
            }
        }
        session.open(runtime)
        geckoView.setSession(session)
    }

    private fun confirmOpen() {
        AlertDialog.Builder(this)
            .setTitle("Открыть Т‑Банк в desktop mode?")
            .setMessage("GeckoView использует официальный режим настольного User-Agent и настольного viewport. Сессия этого приложения отдельна от WebView-прототипа.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Открыть") { _, _ -> session.loadUri(OPERATIONS_URL) }
            .show()
    }

    private fun showBlocked(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Переход заблокирован")
            .setMessage("Основная страница разрешена только на HTTPS-доменах Т‑Банка.\n$url")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        if (canGoBack) session.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        geckoView.releaseSession()
        session.close()
        runtime.shutdown()
        super.onDestroy()
    }
}
