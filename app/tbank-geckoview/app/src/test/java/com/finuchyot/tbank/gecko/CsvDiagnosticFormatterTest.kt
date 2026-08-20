package com.finuchyot.tbank.gecko

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvDiagnosticFormatterTest {
    @Test
    fun formatsOnlyTechnicalMetadata() {
        val result = CsvDiagnosticResult(
            encoding = "UTF-8",
            delimiterLabel = "точка с запятой (;)",
            headers = listOf("Дата", "Категория", "Сумма"),
            rowCount = 2,
            validRowCount = 2,
            errorRowCount = 0,
            firstDate = "01.08.2026",
            lastDate = "03.08.2026"
        )

        val text = CsvDiagnosticFormatter.format("report.csv", 1234, result)

        assertTrue(text.contains("report.csv"))
        assertTrue(text.contains("UTF-8"))
        assertTrue(text.contains("Дата\n• Категория\n• Сумма"))
        assertTrue(text.contains("01.08.2026 — 03.08.2026"))
        assertFalse(text.contains("Магазин"))
        assertFalse(text.contains("-100"))
    }
}
