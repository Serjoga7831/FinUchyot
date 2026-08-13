package com.finuchyot.tbank

object DesktopViewportScript {
    fun build(width: Int): String {
        require(width in 980..2560)
        return """
            (function() {
              var meta = document.querySelector('meta[name="viewport"]');
              if (!meta) {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                document.head.appendChild(meta);
              }
              meta.content = 'width=$width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes';
              return String(window.innerWidth) + 'x' + String(window.innerHeight);
            })();
        """.trimIndent()
    }

    fun parseMetrics(raw: String?): String {
        val match = Regex("^\\\"(\\d{3,4})x(\\d{3,4})\\\"$").matchEntire(raw ?: return "не определён")
            ?: return "не определён"
        return "${match.groupValues[1]} × ${match.groupValues[2]} CSS px"
    }
}
