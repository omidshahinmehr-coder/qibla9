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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
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
import java.util.*

/* -------------------- رنگ‌های پریمیوم -------------------- */

private val bgColor = ColorProvider(Color(0xFFF7EFE3))
private val cellBorderColor = ColorProvider(Color(0xFFCBB88A))
private val cellFillColor = ColorProvider(Color(0xFFFBF6EA))
private val goldText = ColorProvider(Color(0xFF7A5A22))
private val faintGoldText = ColorProvider(Color(0xFFB89A63))

private val widgetPrayerKeys = listOf("Fajr", "Sunrise", "Dhuhr", "Sunset", "Maghrib", "Midnight")
private val cellWidth = 72.dp

class QiblaWidget : GlanceAppWidget() {
override suspend fun provideGlance(context: Context, id: GlanceId) {
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
.cornerRadius(24.dp)
.padding(14.dp)
.clickable(actionStartActivity<MainActivity>()),
horizontalAlignment = Alignment.Horizontal.CenterHorizontally
) {

if (snapshot != null) {

val countdown = nextPrayerCountdown(snapshot.timings)
val weekdayName = weekdayName(language)
val gregorianText = formatGregorian(langContext, snapshot.gregorianDateKey)
val jalaliWithWeekday = listOf(weekdayName, snapshot.jalaliText)
.filter { it.isNotBlank() }
.joinToString(" ")

/* -------------------- ساعت (اصلاح‌شده) -------------------- */
Box(
modifier = GlanceModifier
.fillMaxWidth()
.height(40.dp)
) {
AndroidRemoteViews(
RemoteViews(langContext.packageName, R.layout.widget_clock)
)
}

Spacer(modifier = GlanceModifier.height(6.dp))

Text(
text = jalaliWithWeekday,
style = TextStyle(color = goldText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
)

Spacer(modifier = GlanceModifier.height(6.dp))

Text(
text = "gregorianText",
style = TextStyle(color = faintGoldText, fontSize = 12.sp)
)

Spacer(modifier = GlanceModifier.height(10.dp))

/* -------------------- شمارش معکوس (اصلاح‌شده) -------------------- */
if (countdown != null) {

Text(
text = langContext.getString(
R.string.widget_countdown_label,
labels[countdown.first] ?: countdown.first,
snapshot.cityName
),
style = TextStyle(color = goldText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
)

Spacer(modifier = GlanceModifier.height(4.dp))

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

val nowElapsed = SystemClock.elapsedRealtime()
val nowWall = System.currentTimeMillis()
val base = nowElapsed + (countdown.second - nowWall)

val rv = RemoteViews(langContext.packageName, R.layout.widget_countdown)
rv.setChronometer(R.id.widget_countdown_view, base, null, true)

Box(
modifier = GlanceModifier
.fillMaxWidth()
.height(32.dp)
) {
AndroidRemoteViews(rv)
}

} else {
Text(
text = staticDuration(countdown.second),
style = TextStyle(color = goldText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
)
}
}

if (snapshot.isOffline) {
Text(
text = langContext.getString(R.string.widget_offline_tag),
style = TextStyle(color = faintGoldText, fontSize = 10.sp)
)
}

Spacer(modifier = GlanceModifier.height(12.dp))

/* -------------------- سلول‌های نماز -------------------- */
Row(
modifier = GlanceModifier.fillMaxWidth(),
horizontalAlignment = Alignment.Horizontal.CenterHorizontally
) {
widgetPrayerKeys.forEachIndexed { index, key ->
if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
PrayerCell(
label = labels[key] ?: key,
time = snapshot.timings[key] ?: "--:--"
)
}
}

} else {

Text(
text = langContext.getString(R.string.widget_updating),
style = TextStyle(color = goldText, fontSize = 13.sp)
)

Spacer(modifier = GlanceModifier.height(6.dp))

Text(
text = langContext.getString(R.string.widget_open_app_hint),
style = TextStyle(color = faintGoldText, fontSize = 11.sp)
)
}
}
}

@Composable
private fun PrayerCell(label: String, time: String) {
Column(
modifier = GlanceModifier
.width(cellWidth)
.background(cellBorderColor)
.cornerRadius(18.dp)
.padding(1.5.dp)
) {
Column(
modifier = GlanceModifier
.fillMaxWidth()
.background(cellFillColor)
.cornerRadius(16.dp)
.padding(horizontal = 6.dp, vertical = 8.dp),
horizontalAlignment = Alignment.Horizontal.CenterHorizontally
) {
Text(
text = label,
style = TextStyle(color = goldText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
)

Spacer(modifier = GlanceModifier.height(4.dp))

Text(
text = time,
style = TextStyle(color = goldText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
)
}
}
}

/* -------------------- توابع کمکی -------------------- */

private fun weekdayName(language: String): String {
val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
return when (language) {
"fa" -> arrayOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")[dow - 1]
"ar" -> arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")[dow - 1]
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

return bestKey to bestCal!!.timeInMillis
}
