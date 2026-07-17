package com.hipka.app.core.util

import java.util.Locale

/**
 * فرمت‌کننده تعداد پخش آهنگ
 * مثال: 999 -> 999 | 1500 -> 1.5k | 1200000 -> 1.2M
 */
fun Int.formatPlayCount(): String {
    return when {
        this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000.0).replace(".0M", "M")
        this >= 1_000 -> String.format(Locale.US, "%.1fk", this / 1_000.0).replace(".0k", "k")
        else -> this.toString()
    }
}