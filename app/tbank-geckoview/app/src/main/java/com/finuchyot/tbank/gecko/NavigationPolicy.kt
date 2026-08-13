package com.finuchyot.tbank.gecko

enum class NavigationTarget { NONE, CURRENT, NEW }

object NavigationPolicy {
    fun isAllowed(uri: String, target: NavigationTarget): Boolean =
        TrustedBankUrl.isAllowed(uri) && target != NavigationTarget.NEW
}
