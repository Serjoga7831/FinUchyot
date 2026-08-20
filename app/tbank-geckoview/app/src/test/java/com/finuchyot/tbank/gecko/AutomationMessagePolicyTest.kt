package com.finuchyot.tbank.gecko

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationMessagePolicyTest {
    @Test
    fun acceptsOnlyTopLevelOperationsPageFromCurrentSession() {
        val url = "https://www.tbank.ru/mybank/operations/"
        assertTrue(AutomationMessagePolicy.isTrustedSender(url, isTopLevel = true, sameSession = true))
        assertFalse(AutomationMessagePolicy.isTrustedSender(url, isTopLevel = false, sameSession = true))
        assertFalse(AutomationMessagePolicy.isTrustedSender(url, isTopLevel = true, sameSession = false))
        assertFalse(AutomationMessagePolicy.isTrustedSender("https://www.tbank.ru/login/", true, true))
        assertFalse(AutomationMessagePolicy.isTrustedSender("https://evil.example/mybank/operations/", true, true))
    }

    @Test
    fun acceptsOnlyKnownResultCodes() {
        assertTrue(AutomationMessagePolicy.isKnownCode("ready"))
        assertTrue(AutomationMessagePolicy.isKnownCode("csv_clicked"))
        assertTrue(AutomationMessagePolicy.isKnownCode("share_not_found"))
        assertFalse(AutomationMessagePolicy.isKnownCode("Дата операции: секрет"))
    }
}
