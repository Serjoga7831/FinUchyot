package com.finuchyot.tbank

data class BrowserConfig(
    val startUrl: String,
    val userAgent: String,
    val useWideViewPort: Boolean,
    val loadWithOverviewMode: Boolean
)

object BrowserMode {
    private const val LOGIN_URL = "https://www.tbank.ru/login/"
    private const val OPERATIONS_URL = "https://www.tbank.ru/mybank/operations/"
    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    fun mobile(defaultUserAgent: String) = BrowserConfig(
        startUrl = LOGIN_URL,
        userAgent = defaultUserAgent,
        useWideViewPort = false,
        loadWithOverviewMode = false
    )

    fun desktop() = BrowserConfig(
        startUrl = OPERATIONS_URL,
        userAgent = DESKTOP_USER_AGENT,
        useWideViewPort = true,
        loadWithOverviewMode = true
    )
}
