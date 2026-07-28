package com.qibla.prayertimes.data

import android.content.Context
import com.qibla.prayertimes.R
import com.qibla.prayertimes.util.JalaliCalendar
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WidgetSnapshot(
    val cityName: String,
    val timings: Map<String, String>,
    val hijriText: String,
    val jalaliText: String,
    val gregorianDateKey: String,
    val isOffline: Boolean = false
)

/**
 * Persists the most recently fetched prayer times so they can be read back by:
 *  - the home screen widget (which may render without the app process running), and
 *  - the alarm settings screen, so toggling a prayer on/off can reschedule immediately
 *    using the same data, without a fresh network call.
 */
class WidgetDataStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("qibla_widget_prefs", Context.MODE_PRIVATE)

    fun save(cityName: String, timings: Map<String, String>, hijri: HijriDate?, isOffline: Boolean = false) {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val language = appContext.resources.configuration.locales[0].language
        // The Jalali (Persian solar) calendar is only meaningful to Persian-speaking users;
        // other languages show Gregorian + Hijri only.
        val jalali = if (language == "fa") JalaliCalendar.today().toString() else ""
        val eraSuffix = appContext.getString(R.string.hijri_era_suffix)
        val hijriText = hijri?.let { "${it.day} ${it.monthName(appContext)} ${it.year}$eraSuffix" } ?: ""

        val timingsJson = JSONObject()
        timings.forEach { (k, v) -> timingsJson.put(k, v) }

        prefs.edit()
            .putString(KEY_CITY, cityName)
            .putString(KEY_TIMINGS, timingsJson.toString())
            .putString(KEY_HIJRI_TEXT, hijriText)
            .putString(KEY_JALALI_TEXT, jalali)
            .putString(KEY_DATE_KEY, todayKey)
            .putBoolean(KEY_OFFLINE, isOffline)
            .apply()
    }

    fun load(): WidgetSnapshot? {
        val city = prefs.getString(KEY_CITY, null) ?: return null
        val timingsRaw = prefs.getString(KEY_TIMINGS, null) ?: return null
        val dateKey = prefs.getString(KEY_DATE_KEY, "") ?: ""
        val hijriText = prefs.getString(KEY_HIJRI_TEXT, "") ?: ""
        val jalaliText = prefs.getString(KEY_JALALI_TEXT, "") ?: ""
        val isOffline = prefs.getBoolean(KEY_OFFLINE, false)

        val timings = try {
            val json = JSONObject(timingsRaw)
            val map = mutableMapOf<String, String>()
            json.keys().forEach { key -> map[key] = json.getString(key) }
            map
        } catch (e: Exception) {
            return null
        }

        return WidgetSnapshot(city, timings, hijriText, jalaliText, dateKey, isOffline)
    }

    /** True when the cached snapshot was saved today (Gregorian), i.e. still valid for alarms/widget. */
    fun isFreshToday(): Boolean {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return prefs.getString(KEY_DATE_KEY, null) == todayKey
    }

    companion object {
        private const val KEY_CITY = "city_name"
        private const val KEY_TIMINGS = "timings_json"
        private const val KEY_HIJRI_TEXT = "hijri_text"
        private const val KEY_JALALI_TEXT = "jalali_text"
        private const val KEY_DATE_KEY = "date_key"
        private const val KEY_OFFLINE = "is_offline"
    }
}
