package com.finuchyot.tbank.gecko

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedBankUrlTest {
    @Test
    fun acceptsOnlyHttpsBankHosts() {
        assertTrue(TrustedBankUrl.isAllowed("https://www.tbank.ru/mybank/operations/"))
        assertTrue(TrustedBankUrl.isAllowed("https://id.tbank.ru/auth"))
        assertTrue(TrustedBankUrl.isAllowed("https://www.tinkoff.ru/login"))
        assertFalse(TrustedBankUrl.isAllowed("http://www.tbank.ru/login"))
        assertFalse(TrustedBankUrl.isAllowed("https://tbank.ru.evil.example/login"))
        assertFalse(TrustedBankUrl.isAllowed("javascript:alert(1)"))
    }
}
