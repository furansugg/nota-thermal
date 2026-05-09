package com.notathermal.app.domain

enum class PaperWidth(
    val mm: Float,
    val charsNormal: Int,
    val charsSmall: Int,
    /**
     * Effective printable width in mm (paper width minus the unprintable
     * margins on either side). The DantSu library expects this value (not the
     * paper width) so that `printerWidthPx = printableWidthMm * dpi / 25.4`
     * matches the physical dot count of the print head.
     */
    val printableWidthMm: Float
) {
    MM_58(58f, 32, 42, 48f),
    MM_80(80f, 48, 64, 72f);

    companion object {
        fun fromName(name: String?): PaperWidth = values().firstOrNull { it.name == name } ?: MM_58
    }
}

enum class TextAlign {
    LEFT, CENTER, RIGHT;

    companion object {
        fun fromName(name: String?): TextAlign = values().firstOrNull { it.name == name } ?: CENTER
    }
}
