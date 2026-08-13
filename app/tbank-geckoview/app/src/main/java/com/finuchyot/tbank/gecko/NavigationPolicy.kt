package com.finuchyot.tbank.gecko

import java.net.URI

enum class NavigationTarget { NONE, CURRENT, NEW }

object NavigationPolicy {
    fun isAllowed(uri: String, target: NavigationTarget, triggerUri: String?): Boolean {
        if (target == NavigationTarget.NEW) return false
        if (TrustedBankUrl.isAllowed(uri)) return true
        if (!uri.startsWith("blob:")) return false
        if (target != NavigationTarget.NONE && target != NavigationTarget.CURRENT) return false
        if (triggerUri == null || !TrustedBankUrl.isAllowed(triggerUri)) return false
        return trustedBlobOrigin(uri)
    }

    fun isBlob(uri: String): Boolean = uri.startsWith("blob:")

    fun safeDescription(uri: String, target: NavigationTarget): String {
        val scheme = runCatching { URI(uri).scheme }.getOrNull() ?: "неизвестна"
        val host = when {
            scheme == "https" -> runCatching { URI(uri).host }.getOrNull()
            scheme == "blob" -> runCatching { URI(uri.removePrefix("blob:")).host }.getOrNull()
            else -> null
        }
        return "схема=$scheme, домен=${host ?: "нет"}, окно=${target.name.lowercase()}"
    }

    private fun trustedBlobOrigin(uri: String): Boolean {
        val embedded = uri.removePrefix("blob:")
        return TrustedBankUrl.isAllowed(embedded)
    }
}
