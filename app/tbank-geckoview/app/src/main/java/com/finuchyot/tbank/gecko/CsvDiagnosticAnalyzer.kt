package com.finuchyot.tbank.gecko

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CsvDiagnosticResult(
    val encoding: String,
    val delimiterLabel: String,
    val headers: List<String>,
    val rowCount: Int,
    val validRowCount: Int,
    val errorRowCount: Int,
    val firstDate: String?,
    val lastDate: String?
)

object CsvDiagnosticAnalyzer {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun analyze(bytes: ByteArray): CsvDiagnosticResult {
        require(bytes.isNotEmpty()) { "CSV пуст" }
        val decoded = decode(bytes)
        val text = decoded.first.removePrefix("\uFEFF")
        require(text.isNotBlank()) { "CSV не содержит строк" }
        val delimiter = detectDelimiter(text)
        val records = parseRecords(text, delimiter).filterNot { row -> row.all { it.isBlank() } }
        require(records.isNotEmpty()) { "CSV не содержит строк" }
        val headers = records.first().map { it.trim().trim('\uFEFF') }
        require(headers.size > 1 && headers.any { it.isNotBlank() }) { "Не найдены заголовки CSV" }
        val rows = records.drop(1)
        val dateIndex = headers.indexOfFirst { it.lowercase().contains("дата") }
        val dates = mutableListOf<LocalDate>()
        var valid = 0
        var errors = 0
        for (cells in rows) {
            if (cells.size != headers.size) {
                errors++
                continue
            }
            if (dateIndex >= 0) {
                val date = runCatching {
                    LocalDate.parse(cells[dateIndex].trim().take(10), dateFormatter)
                }.getOrNull()
                if (date == null) {
                    errors++
                    continue
                }
                dates.add(date)
            }
            valid++
        }
        return CsvDiagnosticResult(
            encoding = decoded.second,
            delimiterLabel = delimiterLabel(delimiter),
            headers = headers,
            rowCount = rows.size,
            validRowCount = valid,
            errorRowCount = errors,
            firstDate = dates.minOrNull()?.format(dateFormatter),
            lastDate = dates.maxOrNull()?.format(dateFormatter)
        )
    }

    private fun decode(bytes: ByteArray): Pair<String, String> {
        val hasUtf8Bom = bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        val utf8 = runCatching {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrNull()
        return if (utf8 != null) {
            utf8 to if (hasUtf8Bom) "UTF-8 BOM" else "UTF-8"
        } else {
            bytes.toString(charset("windows-1251")) to "Windows-1251"
        }
    }

    private fun detectDelimiter(text: String): Char {
        val firstRecord = buildString {
            var quoted = false
            var index = 0
            while (index < text.length) {
                val char = text[index]
                if (char == '"') {
                    if (quoted && index + 1 < text.length && text[index + 1] == '"') {
                        append("\"\"")
                        index += 2
                        continue
                    }
                    quoted = !quoted
                }
                if (!quoted && (char == '\n' || char == '\r')) break
                append(char)
                index++
            }
        }
        val candidates = listOf(';', ',', '\t')
        return candidates.maxByOrNull { countOutsideQuotes(firstRecord, it) }
            ?.takeIf { countOutsideQuotes(firstRecord, it) > 0 }
            ?: throw IllegalArgumentException("Не удалось определить разделитель CSV")
    }

    private fun countOutsideQuotes(record: String, delimiter: Char): Int {
        var quoted = false
        var count = 0
        var index = 0
        while (index < record.length) {
            val char = record[index]
            if (char == '"') {
                if (quoted && index + 1 < record.length && record[index + 1] == '"') {
                    index += 2
                    continue
                }
                quoted = !quoted
            } else if (!quoted && char == delimiter) {
                count++
            }
            index++
        }
        return count
    }

    private fun parseRecords(text: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> {
                    row.add(cell.toString())
                    cell.setLength(0)
                }
                (char == '\n' || char == '\r') && !quoted -> {
                    row.add(cell.toString())
                    cell.setLength(0)
                    records.add(row.toList())
                    row.clear()
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                }
                else -> cell.append(char)
            }
            index++
        }
        require(!quoted) { "Незакрытая кавычка в CSV" }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            records.add(row.toList())
        }
        return records
    }

    private fun delimiterLabel(delimiter: Char): String = when (delimiter) {
        ';' -> "точка с запятой (;)"
        ',' -> "запятая (,)"
        '\t' -> "табуляция (tab)"
        else -> delimiter.toString()
    }
}
