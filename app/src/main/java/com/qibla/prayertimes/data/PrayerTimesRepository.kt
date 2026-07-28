package com.qibla.prayertimes.data

import android.content.Context
import com.qibla.prayertimes.R
import com.qibla.prayertimes.util.HijriCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** [monthAr] is used for both Arabic and Persian locales (they share Hijri month names); [monthEn] for English. */
data class HijriDate(val day: String, val monthAr: String, val monthEn: String, val year: String) {
    fun monthName(context: Context): String =
        if (context.resources.configuration.locales[0].language == "en") monthEn else monthAr
}

data class PrayerTimesResult(
    val timings: Map<String, String>,
    val hijri: HijriDate?,
    /** True when these times came from the local offline calculator, not the network API. */
    val isOffline: Boolean = false
)

sealed class PrayerTimesState {
    object Loading : PrayerTimesState()
    data class Success(val result: PrayerTimesResult) : PrayerTimesState()
    object Error : PrayerTimesState()
}

/** Order in which prayer times are displayed, matching the labels below. */
val PRAYER_ORDER = listOf("Imsak", "Fajr", "Sunrise", "Dhuhr", "Asr", "Sunset", "Maghrib", "Isha", "Midnight")

private val PRAYER_LABEL_RES = mapOf(
    "Imsak" to R.string.prayer_imsak,
    "Fajr" to R.string.prayer_fajr,
    "Sunrise" to R.string.prayer_sunrise,
    "Dhuhr" to R.string.prayer_dhuhr,
    "Asr" to R.string.prayer_asr,
    "Sunset" to R.string.prayer_sunset,
    "Maghrib" to R.string.prayer_maghrib,
    "Isha" to R.string.prayer_isha,
    "Midnight" to R.string.prayer_midnight
)

/** Localized prayer names, following the app's current language (Persian/English/Arabic). */
fun prayerLabels(context: Context): Map<String, String> =
    PRAYER_LABEL_RES.mapValues { (_, resId) -> context.getString(resId) }

class PrayerTimesRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches today's prayer times from the Aladhan API (method=7, University of Tehran).
     * If the network is unreachable or the request fails for any reason, silently falls back
     * to [OfflinePrayerCalculator] so the app still works with no internet connection — the
     * result is flagged with [PrayerTimesResult.isOffline] so the UI can note it's approximate.
     */
    suspend fun fetchToday(lat: Double, lon: Double): PrayerTimesState = withContext(Dispatchers.IO) {
        try {
            fetchOnline(lat, lon)
        } catch (e: Exception) {
            try {
                PrayerTimesState.Success(computeOffline(lat, lon))
            } catch (offlineError: Exception) {
                PrayerTimesState.Error
            }
        }
    }

    private fun fetchOnline(lat: Double, lon: Double): PrayerTimesState {
        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
        val url = "https://api.aladhan.com/v1/timings/$dateStr?latitude=$lat&longitude=$lon&method=7&midnightMode=1"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return PrayerTimesState.Success(computeOffline(lat, lon))
            }
            val body = response.body?.string()
                ?: return PrayerTimesState.Success(computeOffline(lat, lon))
            val json = JSONObject(body)
            val data = json.getJSONObject("data")
            val timingsJson = data.getJSONObject("timings")
            val timings = mutableMapOf<String, String>()
            for (key in PRAYER_ORDER) {
                val raw = timingsJson.optString(key, "--:--")
                timings[key] = raw.split(" ").firstOrNull() ?: raw
            }
            val hijriJson = data.optJSONObject("date")?.optJSONObject("hijri")
            val hijri = hijriJson?.let {
                HijriDate(
                    day = it.optString("day", ""),
                    monthAr = it.optJSONObject("month")?.optString("ar", "") ?: "",
                    monthEn = it.optJSONObject("month")?.optString("en", "") ?: "",
                    year = it.optString("year", "")
                )
            }
            return PrayerTimesState.Success(PrayerTimesResult(timings, hijri, isOffline = false))
        }
    }

    private fun computeOffline(lat: Double, lon: Double): PrayerTimesResult {
        val timings = OfflinePrayerCalculator.computeToday(lat, lon)
        val h = HijriCalendar.today()
        val hijri = HijriDate(day = h.day.toString(), monthAr = h.monthNameAr, monthEn = h.monthNameEn, year = h.year.toString())
        return PrayerTimesResult(timings, hijri, isOffline = true)
    }
}
