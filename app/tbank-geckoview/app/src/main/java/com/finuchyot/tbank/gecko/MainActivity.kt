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
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession
    private lateinit var runtime: GeckoRuntime
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private var canGoBack = false
    private var pendingBlobUri: String? = null
    private val downloadExecutor = Executors.newSingleThreadExecutor()

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
        controls.addView(Button(this).apply {
            text = "Отчёты"
            setOnClickListener { showReports() }
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
                val target = when (request.target) {
                    GeckoSession.NavigationDelegate.TARGET_WINDOW_CURRENT -> NavigationTarget.CURRENT
                    GeckoSession.NavigationDelegate.TARGET_WINDOW_NONE -> NavigationTarget.NONE
                    else -> NavigationTarget.NEW
                }
                return if (NavigationPolicy.isAllowed(request.uri, target, request.triggerUri)) {
                    if (NavigationPolicy.isBlob(request.uri)) {
                        pendingBlobUri = request.uri
                    } else if (target == NavigationTarget.CURRENT) {
                        pendingBlobUri = null
                        status.text = "Официальный адрес: ${java.net.URI(request.uri).host}"
                    }
                    GeckoResult.allow()
                } else {
                    val detail = NavigationPolicy.safeDescription(request.uri, target)
                    runOnUiThread { showBlocked(detail) }
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
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val trustedBlob = pendingBlobUri != null && response.uri == pendingBlobUri
                pendingBlobUri = null
                if (!CsvDownloadPolicy.accepts(response.uri, response.headers, trustedBlob)) {
                    response.body?.close()
                    runOnUiThread {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Загрузка отклонена")
                            .setMessage("FinUchyot принимает только CSV с HTTPS-домена Т‑Банка. Выберите CSV, а не Excel или OFX.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    return
                }
                saveCsv(response)
            }
        }
        session.open(runtime)
        geckoView.setSession(session)
    }

    private fun saveCsv(response: WebResponse) {
        downloadExecutor.execute {
            val reports = File(filesDir, "reports").apply { mkdirs() }
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val target = File(reports, "tbank-operations-$stamp.csv")
            try {
                val body = response.body ?: throw IllegalStateException("Ответ CSV не содержит данных")
                body.use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > CsvDownloadPolicy.MAX_BYTES) {
                                throw IllegalStateException("CSV превышает допустимый размер")
                            }
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
                runOnUiThread {
                    Toast.makeText(this, "CSV сохранён: ${target.name}", Toast.LENGTH_LONG).show()
                    showReports()
                }
            } catch (error: Exception) {
                target.delete()
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Не удалось сохранить CSV")
                        .setMessage(error.message ?: "Неизвестная ошибка")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun showReports() {
        val reports = File(filesDir, "reports")
            .listFiles { file -> file.isFile && file.extension.lowercase() == "csv" }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        if (reports.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Локальные отчёты")
                .setMessage("Сохранённых CSV пока нет. В экспорте Т‑Банка выберите CSV.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val labels = reports.map { "${it.name} — ${it.length()} байт" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Выберите CSV для анализа")
            .setItems(labels) { _, index -> analyzeReport(reports[index]) }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun analyzeReport(file: File) {
        Toast.makeText(this, "Анализ CSV…", Toast.LENGTH_SHORT).show()
        downloadExecutor.execute {
            try {
                require(file.isFile && file.parentFile == File(filesDir, "reports")) {
                    "Недопустимый путь отчёта"
                }
                require(file.length() <= CsvDownloadPolicy.MAX_BYTES) {
                    "CSV превышает допустимый размер"
                }
                val result = CsvDiagnosticAnalyzer.analyze(file.readBytes())
                val summary = CsvDiagnosticFormatter.format(file.name, file.length(), result)
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Диагностика CSV")
                        .setMessage(summary)
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("CSV не распознан")
                        .setMessage(error.message ?: "Не удалось определить структуру CSV")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun confirmOpen() {
        AlertDialog.Builder(this)
            .setTitle("Открыть Т‑Банк в desktop mode?")
            .setMessage("GeckoView использует официальный режим настольного User-Agent и настольного viewport. Сессия этого приложения отдельна от WebView-прототипа.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Открыть") { _, _ -> session.loadUri(OPERATIONS_URL) }
            .show()
    }

    private fun showBlocked(detail: String) {
        AlertDialog.Builder(this)
            .setTitle("Переход заблокирован")
            .setMessage("Разрешены страницы Т‑Банка и доверенные CSV-загрузки.\n$detail")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        if (canGoBack) session.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        downloadExecutor.shutdownNow()
        geckoView.releaseSession()
        session.close()
        runtime.shutdown()
        super.onDestroy()
    }
}
