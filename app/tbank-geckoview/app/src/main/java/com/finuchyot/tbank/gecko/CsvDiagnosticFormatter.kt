package com.finuchyot.tbank.gecko

object CsvDiagnosticFormatter {
    fun format(fileName: String, fileSize: Long, result: CsvDiagnosticResult): String {
        val dates = when {
            result.firstDate != null && result.lastDate != null ->
                "${result.firstDate} — ${result.lastDate}"
            result.firstDate != null -> result.firstDate
            else -> "не определён"
        }
        val headers = result.headers.joinToString("\n") { "• $it" }
        return """
            Файл: $fileName
            Размер: $fileSize байт
            Кодировка: ${result.encoding}
            Разделитель: ${result.delimiterLabel}
            Строк операций: ${result.rowCount}
            Корректных строк: ${result.validRowCount}
            Строк с ошибками: ${result.errorRowCount}
            Диапазон дат: $dates

            Колонки:
            $headers

            Суммы, назначения и значения операций здесь не отображаются.
        """.trimIndent()
    }
}
