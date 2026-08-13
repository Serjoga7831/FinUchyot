package com.finuchyot.tbank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopViewportScriptTest {
    @Test
    fun scriptForcesDesktopViewportWithoutReadingPageContent() {
        val script = DesktopViewportScript.build(1280)

        assertTrue(script.contains("width=1280"))
        assertTrue(script.contains("document.head.appendChild"))
        assertTrue(script.contains("window.innerWidth"))
        assertFalse(script.contains("querySelector('input"))
        assertFalse(script.contains("document.cookie"))
        assertFalse(script.contains("localStorage"))
    }

    @Test
    fun resultParserAcceptsOnlyViewportMetrics() {
        assertEquals("1280 × 720 CSS px", DesktopViewportScript.parseMetrics("\"1280x720\""))
        assertEquals("не определён", DesktopViewportScript.parseMetrics("null"))
        assertEquals("не определён", DesktopViewportScript.parseMetrics("\"secret data\""))
    }
}
