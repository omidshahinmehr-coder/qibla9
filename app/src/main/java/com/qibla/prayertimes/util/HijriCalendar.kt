package com.qibla.prayertimes.util

import java.util.Calendar

data class HijriApprox(val year: Int, val month: Int, val day: Int) {
    companion object {
        // Shared by Persian and Arabic — both languages use the same Hijri month names.
        private val MONTH_NAMES_AR = listOf(
            "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی",
            "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
        )
        private val MONTH_NAMES_EN = listOf(
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani",
            "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhu al-Qidah", "Dhu al-Hijjah"
        )
    }

    val monthNameAr: String get() = MONTH_NAMES_AR.getOrElse(month - 1) { "" }
    val monthNameEn: String get() = MONTH_NAMES_EN.getOrElse(month - 1) { "" }
}

/**
 * Approximate Gregorian-to-Hijri conversion using the standard tabular (civil) Islamic
 * calendar algorithm — a fixed 30-year/11-leap-year arithmetic cycle, with no network call.
 *
 * This is only used offline, as a fallback for the moon-sighting-based Hijri date normally
 * supplied by the prayer-times API. It is accurate to within about a day of the officially
 * announced date (verified against the well-known epoch 30 Jul 2022 = 1 Muharram 1444 AH),
 * which is why it is always labeled "تقریبی" (approximate) in the UI rather than shown as
 * the authoritative date.
 */
object HijriCalendar {

    fun today(): HijriApprox {
        val cal = Calendar.getInstance()
        return fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun fromGregorian(gy: Int, gm: Int, gd: Int): HijriApprox {
        val jdn = gregorianToJdn(gy, gm, gd)
        val (y, m, d) = jdnToIslamic(jdn)
        return HijriApprox(y, m, d)
    }

    private fun gregorianToJdn(y: Int, m: Int, d: Int): Long {
        val a = (14 - m) / 12
        val y2 = y + 4800 - a
        val m2 = m + 12 * a - 3
        return (d + (153L * m2 + 2) / 5 + 365L * y2 + y2 / 4 - y2 / 100 + y2 / 400 - 32045).toLong()
    }

    private fun jdnToIslamic(jdn0: Long): Triple<Int, Int, Int> {
        var jdn = jdn0 - 1948440 + 10632
        val n = (jdn - 1) / 10631
        jdn = jdn - 10631 * n + 354
        val j = ((10985 - jdn) / 5316) * ((50 * jdn) / 17719) + (jdn / 5670) * ((43 * jdn) / 15238)
        jdn = jdn - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val month = (24 * jdn) / 709
        val day = jdn - (709 * month) / 24
        val year = 30 * n + j - 30
        return Triple(year.toInt(), month.toInt(), day.toInt())
    }
}
