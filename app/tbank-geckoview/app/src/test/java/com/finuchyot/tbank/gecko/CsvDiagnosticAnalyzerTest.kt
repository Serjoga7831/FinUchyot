package com.finuchyot.tbank.gecko

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvDiagnosticAnalyzerTest {
    @Test
    fun analyzesUtf8SemicolonCsvWithoutExposingValues() {
        val csv = "Дата операции;Категория;Сумма\n" +
            "01.08.2026;Транспорт;-100,00\n" +
            "03.08.2026;Супермаркеты;-250,00\n"

        val result = CsvDiagnosticAnalyzer.analyze(csv.toByteArray(Charsets.UTF_8))

        assertEquals("UTF-8", result.encoding)
        assertEquals("точка с запятой (;)" , result.delimiterLabel)
        assertEquals(listOf("Дата операции", "Категория", "Сумма"), result.headers)
        assertEquals(2, result.rowCount)
        assertEquals(2, result.validRowCount)
        assertEquals(0, result.errorRowCount)
        assertEquals("01.08.2026", result.firstDate)
        assertEquals("03.08.2026", result.lastDate)
    }

    @Test
    fun parsesCommaCsvWithQuotedDelimiter() {
        val csv = "Дата,Категория,Описание\n" +
            "01.08.2026,Покупки,\"Магазин, отдел продуктов\"\n"

        val result = CsvDiagnosticAnalyzer.analyze(csv.toByteArray())

        assertEquals("запятая (,)", result.delimiterLabel)
        assertEquals(listOf("Дата", "Категория", "Описание"), result.headers)
        assertEquals(1, result.validRowCount)
        assertEquals(0, result.errorRowCount)
    }

    @Test
    fun countsMalformedRowsAsErrors() {
        val csv = "Дата;Категория;Сумма\n" +
            "не-дата;Транспорт;-100,00\n" +
            "02.08.2026;Лишняя;Колонка;Ошибка\n"

        val result = CsvDiagnosticAnalyzer.analyze(csv.toByteArray())

        assertEquals(2, result.rowCount)
        assertEquals(0, result.validRowCount)
        assertEquals(2, result.errorRowCount)
    }

    @Test
    fun detectsWindows1251() {
        val csv = "Дата;Категория;Сумма\n01.08.2026;Транспорт;-100,00\n"
        val bytes = csv.toByteArray(charset("windows-1251"))

        val result = CsvDiagnosticAnalyzer.analyze(bytes)

        assertEquals("Windows-1251", result.encoding)
        assertEquals(listOf("Дата", "Категория", "Сумма"), result.headers)
        assertEquals(1, result.validRowCount)
    }
}
