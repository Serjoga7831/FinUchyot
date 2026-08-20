package com.finuchyot.tbank.gecko

import java.net.URI

object AutomationMessagePolicy {
    private val knownCodes = setOf(
        "ready",
        "busy",
        "wrong_page",
        "share_not_found",
        "share_ambiguous",
        "csv_not_found",
        "csv_ambiguous",
        "csv_clicked",
        "automation_error"
    )

    fun isTrustedSender(url: String?, isTopLevel: Boolean, sameSession: Boolean): Boolean {
        if (!isTopLevel || !sameSession || url == null || !TrustedBankUrl.isAllowed(url)) return false
        val path = runCatching { URI(url).path }.getOrNull() ?: return false
        return path == "/mybank/operations" || path.startsWith("/mybank/operations/")
    }

    fun isKnownCode(code: String): Boolean = code in knownCodes

    fun userMessage(code: String): String = when (code) {
        "ready" -> "Автоматизация готова"
        "busy" -> "Автоматизация уже выполняется"
        "wrong_page" -> "Откройте страницу «Операции»"
        "share_not_found" -> "Не найден элемент «Поделиться». Проверьте, что список операций загружен."
        "share_ambiguous" -> "Найдено несколько элементов «Поделиться». Автоматизация остановлена."
        "csv_not_found" -> "Меню открылось, но пункт CSV не найден."
        "csv_ambiguous" -> "Найдено несколько пунктов CSV. Автоматизация остановлена."
        "csv_clicked" -> "CSV выбран, ожидается сохранение файла"
        else -> "Автоматизация остановлена из-за ошибки"
    }
}
