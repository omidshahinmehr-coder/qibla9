package com.qibla.prayertimes.widget

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.qibla.prayertimes.MainActivity
import com.qibla.prayertimes.R
import com.qibla.prayertimes.data.WidgetDataStore
import com.qibla.prayertimes.data.WidgetSnapshot
import com.qibla.prayertimes.data.prayerLabels
import com.qibla.prayertimes.util.LocalePrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Light cream / antique-gold palette, matching the requested widget design.
private val bgColor = ColorProvider(Color(0xFFF3ECDD))
private val cellBorderColor = ColorProvider(Color(0xFFD9C8A0))
private val cellFillColor = ColorProvider(Color(0xFFFBF6EA))
private val goldText = ColorProvider(Color(0xFF8A6A2E))
private val faintGoldText = ColorProvider(Color(0xFFAD8F55))

// Same six as the home screen — no Asr, no Isha.
private val widgetPrayerKeys = listOf("Fajr", "Sunrise", "Dhuhr", "Sunset", "Maghrib", "Midnight")
private val cellWidth = 66.dp

private val WEEKDAYS_FA = arrayOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")
private val WEEKDAYS_AR = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

class QiblaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Follow the app's own language override (not just the system language), the same way
        // MainActivity does — a plain Glance context otherwise ignores that in-app choice.
        val localizedContext = LocalePrefs.wrap(context)
        val snapshot = WidgetDataStore(context).load()
        provideContent {
            WidgetContent(localizedContext, snapshot)
        }
    }
}

@Composable
private fun WidgetContent(langContext: Context, snapshot: WidgetSnapshot?) {
    val labels = prayerLabels(langContext)
    val language = langContext.resources.configuration.locales[0].language

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        if (snapshot != null) {
            val countdown = nextPrayerCountdown(snapshot.timings)
            val weekdayName = weekdayName(language)
            val gregorianText = formatGregorian(langContext, snapshot.gregorianDateKey)
            val jalaliWithWeekday = listOf(weekdayName, snapshot.jalaliText).filter { it.isNotBlank() }.joinToString(" ")

            // Row 1: clock, HH:mm:ss
            AndroidRemoteViews(RemoteViews(langContext.packageName, R.layout.widget_clock))

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Row 2: Jalali date with weekday
            Text(
                text = jalaliWithWeekday,
                style = TextStyle(color = goldText, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            )

            Spacer(modifier = GlanceModifier.height(3.dp))

            // Row 3: Hijri date — Gregorian date
            Text(
                text = "${snapshot.hijriText} - $gregorianText",
                style = TextStyle(color = faintGoldText, fontSize = 11.sp, textAlign = TextAlign.Center)
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Row 4: countdown to the next prayer
            if (countdown != null) {
                Text(
                    text = langContext.getString(R.string.widget_countdown_label, labels[countdown.first] ?: countdown.first, snapshot.cityName),
                    style = TextStyle(color = goldText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val nowElapsed = SystemClock.elapsedRealtime()
                    val nowWall = System.currentTimeMillis()
                    val base = nowElapsed + (countdown.second - nowWall)
                    val rv = RemoteViews(langContext.packageName, R.layout.widget_countdown)
                    rv.setChronometer(R.id.widget_countdown_view, base, null, true)
                    AndroidRemoteViews(rv)
                } else {
                    Text(
                        text = staticDuration(countdown.second),
                        style = TextStyle(color = goldText, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    )
                }
            }

            if (snapshot.isOffline) {
                Text(
                    text = langContext.getString(R.string.widget_offline_tag),
                    style = TextStyle(color = faintGoldText, fontSize = 9.sp, textAlign = TextAlign.Center)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Row 5: prayer times
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                widgetPrayerKeys.forEachIndexed { index, key ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(4.dp))
                    PrayerCell(label = labels[key] ?: key, time = snapshot.timings[key] ?: "--:--")
                }
            }
        } else {
            Text(
                text = langContext.getString(R.string.widget_updating),
                style = TextStyle(color = goldText, fontSize = 13.sp, textAlign = TextAlign.Center)
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = langContext.getString(R.string.widget_open_app_hint),
                style = TextStyle(color = faintGoldText, fontSize = 11.sp, textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
private fun PrayerCell(label: String, time: String) {
    // A thin "border" is faked with two nested rounded boxes, since Glance has no border()
    // modifier of its own.
    Column(
        modifier = GlanceModifier
            .width(cellWidth)
            .background(cellBorderColor)
            .cornerRadius(16.dp)
            .padding(1.2.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(cellFillColor)
                .cornerRadius(15.dp)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(text = label, style = TextStyle(color = goldText, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(text = time, style = TextStyle(color = goldText, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        }
    }
}

private fun weekdayName(language: String): String {
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // 1=Sunday..7=Saturday
    return when (language) {
        "fa" -> WEEKDAYS_FA[dow - 1]
        "ar" -> WEEKDAYS_AR[dow - 1]
        else -> SimpleDateFormat("EEEE", Locale.ENGLISH).format(Calendar.getInstance().time)
    }
}

private fun formatGregorian(context: Context, dateKey: String): String {
    return try {
        val parts = dateKey.split("-").map { it.toInt() }
        val cal = Calendar.getInstance().apply { set(parts[0], parts[1] - 1, parts[2]) }
        SimpleDateFormat("d MMMM yyyy", context.resources.configuration.locales[0]).format(cal.time)
    } catch (e: Exception) {
        dateKey
    }
}

private fun staticDuration(targetMillis: Long): String {
    val diff = (targetMillis - System.currentTimeMillis()).coerceAtLeast(0) / 1000
    val h = diff / 3600
    val m = (diff % 3600) / 60
    val s = diff % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/**
 * Finds the next of the five canonical prayers (Fajr/Dhuhr/Asr/Maghrib/Isha) from now.
 * Returns (prayerKey, targetEpochMillis). If every one of today's times has already passed,
 * falls back to today's Fajr time again as a same-time-tomorrow approximation (Fajr barely
 * shifts day to day), rather than showing nothing.
 */
private fun nextPrayerCountdown(timings: Map<String, String>): Pair<String, Long>? {
    val order = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    val now = Calendar.getInstance()
    val sdf = SimpleDateFormat("HH:mm", Locale.US)

    fun toCalendar(hhmm: String): Calendar? {
        val parsed = try { sdf.parse(hhmm) } catch (e: Exception) { null } ?: return null
        val parsedCal = Calendar.getInstance().apply { time = parsed }
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var bestKey: String? = null
    var bestCal: Calendar? = null
    for (key in order) {
        val timeStr = timings[key] ?: continue
        val cal = toCalendar(timeStr) ?: continue
        if (cal.after(now) && (bestCal == null || cal.before(bestCal))) {
            bestKey = key
            bestCal = cal
        }
    }
    if (bestKey == null) {
        val fajrStr = timings["Fajr"] ?: return null
        val cal = toCalendar(fajrStr) ?: return null
        cal.add(Calendar.DAY_OF_YEAR, 1)
        bestKey = "Fajr"
        bestCal = cal
    }
    val target = bestCal ?: return null
    return bestKey to target.timeInMillis
}
