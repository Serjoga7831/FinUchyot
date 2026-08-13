package com.finuchyot.tbank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserModeTest {
    @Test
    fun desktopModeUsesDesktopChromeAndOperationsPage() {
        val config = BrowserMode.desktop()

        assertEquals("https://www.tbank.ru/mybank/operations/", config.startUrl)
        assertTrue(config.userAgent.contains("X11; Linux x86_64"))
        assertTrue(config.useWideViewPort)
        assertTrue(config.loadWithOverviewMode)
        assertFalse(config.userAgent.contains("Android"))
        assertFalse(config.userAgent.contains("; wv"))
    }

    @Test
    fun mobileModeKeepsDefaultUserAgentAndLoginPage() {
        val config = BrowserMode.mobile("Android-WebView-UA")

        assertEquals("https://www.tbank.ru/login/", config.startUrl)
        assertEquals("Android-WebView-UA", config.userAgent)
        assertFalse(config.useWideViewPort)
        assertFalse(config.loadWithOverviewMode)
    }
}
