package com.notathermal.app.domain

enum class PaperWidth(val mm: Float, val charsNormal: Int, val charsSmall: Int) {
    MM_58(58f, 32, 42),
    MM_80(80f, 48, 64);

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
