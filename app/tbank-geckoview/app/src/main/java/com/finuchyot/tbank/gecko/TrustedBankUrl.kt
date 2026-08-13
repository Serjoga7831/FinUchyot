package com.finuchyot.tbank.gecko

import java.net.URI

object TrustedBankUrl {
    private val suffixes = setOf("tbank.ru", "tinkoff.ru")

    fun isAllowed(raw: String): Boolean = try {
        val uri = URI(raw)
        val host = uri.host?.lowercase() ?: return false
        uri.scheme == "https" && suffixes.any { host == it || host.endsWith(".$it") }
    } catch (_: Exception) {
        false
    }
}
