package com.finuchyot.tbank.gecko

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvDownloadPolicyTest {
    @Test
    fun acceptsTrustedCsvResponses() {
        assertTrue(CsvDownloadPolicy.accepts(
            "https://api.tbank.ru/export/file",
            mapOf("content-type" to "text/csv; charset=utf-8")
        ))
        assertTrue(CsvDownloadPolicy.accepts(
            "https://www.tbank.ru/file",
            mapOf("Content-Disposition" to "attachment; filename=operations.csv")
        ))
        assertTrue(CsvDownloadPolicy.accepts(
            "blob:https://www.tbank.ru/opaque-id",
            mapOf("content-type" to "text/csv"),
            trustedBlob = true
        ))
    }

    @Test
    fun rejectsForeignOrNonCsvResponses() {
        assertFalse(CsvDownloadPolicy.accepts(
            "https://evil.example/export.csv", mapOf("content-type" to "text/csv")
        ))
        assertFalse(CsvDownloadPolicy.accepts(
            "blob:https://www.tbank.ru/opaque-id",
            mapOf("content-type" to "text/csv"),
            trustedBlob = false
        ))
        assertFalse(CsvDownloadPolicy.accepts(
            "https://www.tbank.ru/export", mapOf("content-type" to "application/pdf")
        ))
    }

    @Test
    fun producesSafeLocalName() {
        assertEquals("tbank-operations.csv", CsvDownloadPolicy.safeFileName("../../operations.csv"))
        assertEquals("tbank-operations.csv", CsvDownloadPolicy.safeFileName(null))
    }
}
