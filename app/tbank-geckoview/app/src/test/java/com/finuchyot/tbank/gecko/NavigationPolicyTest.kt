package com.finuchyot.tbank.gecko

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun allowsTrustedCurrentPageAndDownloadTarget() {
        val url = "https://www.tbank.ru/export/operations.csv"
        assertTrue(NavigationPolicy.isAllowed(url, NavigationTarget.CURRENT))
        assertTrue(NavigationPolicy.isAllowed(url, NavigationTarget.NONE))
    }

    @Test
    fun blocksNewWindowsAndForeignDownloads() {
        assertFalse(NavigationPolicy.isAllowed("https://www.tbank.ru/export", NavigationTarget.NEW))
        assertFalse(NavigationPolicy.isAllowed("https://evil.example/export.csv", NavigationTarget.NONE))
    }
}
