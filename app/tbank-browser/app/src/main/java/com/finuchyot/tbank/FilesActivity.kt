package com.finuchyot.tbank

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.DateFormat
import java.util.Date

class FilesActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Загруженные отчёты"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        val note = TextView(this).apply {
            text = "Файлы находятся в изолированной папке приложения. Можно передать выбранный файл в FinUchyot или удалить его."
            setPadding(0, 0, 0, 16)
        }
        val refresh = Button(this).apply {
            text = "Обновить список"
            setOnClickListener { refresh() }
        }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(note)
        root.addView(refresh)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        refresh()
    }

    private fun reportDirectory(): File = File(
        getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports"
    ).apply { mkdirs() }

    private fun refresh() {
        list.removeAllViews()
        val files = reportDirectory().listFiles()?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }.orEmpty()
        if (files.isEmpty()) {
            list.addView(TextView(this).apply { text = "Загруженных отчётов пока нет." })
            return
        }
        files.forEach { file -> list.addView(fileRow(file)) }
    }

    private fun fileRow(file: File): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 18)
        }
        box.addView(TextView(this).apply {
            text = "${file.name}\n${file.length()} байт · ${DateFormat.getDateTimeInstance().format(Date(file.lastModified()))}"
        })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Поделиться"
            setOnClickListener { share(file) }
        })
        actions.addView(Button(this).apply {
            text = "Удалить"
            setOnClickListener {
                if (file.delete()) refresh()
                else Toast.makeText(this@FilesActivity, "Не удалось удалить файл", Toast.LENGTH_SHORT).show()
            }
        })
        box.addView(actions)
        return box
    }

    private fun share(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Передать отчёт"))
    }
}
