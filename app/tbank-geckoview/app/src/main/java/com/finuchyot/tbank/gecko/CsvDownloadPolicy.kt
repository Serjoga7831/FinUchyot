package com.finuchyot.tbank.gecko

object CsvDownloadPolicy {
    const val MAX_BYTES: Long = 25L * 1024L * 1024L

    fun accepts(uri: String, headers: Map<String, String>): Boolean {
        if (!TrustedBankUrl.isAllowed(uri)) return false
        val normalized = headers.entries.associate { it.key.lowercase() to it.value.lowercase() }
        val type = normalized["content-type"].orEmpty()
        val disposition = normalized["content-disposition"].orEmpty()
        return type.contains("text/csv") ||
            type.contains("application/csv") ||
            type.contains("application/vnd.ms-excel") ||
            disposition.contains(".csv")
    }

    fun safeFileName(untrusted: String?): String = "tbank-operations.csv"
}
