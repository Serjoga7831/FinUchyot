package com.finuchyot.tbank.gecko

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun allowsTrustedCurrentPageAndDownloadTarget() {
        val url = "https://www.tbank.ru/export/operations.csv"
        assertTrue(NavigationPolicy.isAllowed(url, NavigationTarget.CURRENT, null))
        assertTrue(NavigationPolicy.isAllowed(url, NavigationTarget.NONE, null))
        assertTrue(NavigationPolicy.isAllowed(
            "blob:https://www.tbank.ru/opaque-id",
            NavigationTarget.NONE,
            "https://www.tbank.ru/mybank/operations/"
        ))
    }

    @Test
    fun blocksNewWindowsAndUntrustedDownloads() {
        assertFalse(NavigationPolicy.isAllowed("https://www.tbank.ru/export", NavigationTarget.NEW, null))
        assertFalse(NavigationPolicy.isAllowed("https://evil.example/export.csv", NavigationTarget.NONE, null))
        assertFalse(NavigationPolicy.isAllowed(
            "blob:https://www.tbank.ru/opaque-id",
            NavigationTarget.CURRENT,
            "https://www.tbank.ru/mybank/operations/"
        ))
        assertFalse(NavigationPolicy.isAllowed(
            "blob:https://evil.example/opaque-id",
            NavigationTarget.NONE,
            "https://evil.example/operations/"
        ))
        assertFalse(NavigationPolicy.isAllowed(
            "data:text/csv,secret",
            NavigationTarget.NONE,
            "https://www.tbank.ru/mybank/operations/"
        ))
    }
}
