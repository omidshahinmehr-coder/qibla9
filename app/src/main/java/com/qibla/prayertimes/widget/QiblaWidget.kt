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
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.qibla.prayertimes.MainActivity
import com.qibla.prayertimes.R
import com.qibla.prayertimes.data.WidgetDataStore
import com.qibla.prayertimes.data.WidgetSnapshot
import com.qibla.prayertimes.data.prayerLabels
import com.qibla.prayertimes.util.LocalePrefs
import java.text.SimpleDateFormat
import java.util.*

private val bgColor = ColorProvider(Color(0xFFF3ECDD))
private val cellBorderColor = ColorProvider(Color(0xFFD9C8A0))
private val cellFillColor = ColorProvider(Color(0xFFFBF6EA))
private val goldText = ColorProvider(Color(0xFF8A6A2E))
private val faintGoldText = ColorProvider(Color(0xFFAD8F55))

private val widgetPrayerKeys = listOf("Fajr", "Sunrise", "Dhuhr", "Sunset", "Maghrib", "Midnight")
private val cellWidth = 66.dp

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
val gregorianText = formatGregorian(langContext, snapshot?.gregorianDateKey ?: "")

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

val clockBlock: @Composable () -> Unit = {
AndroidRemoteViews(RemoteViews(langContext.packageName, R.layout.widget_clock))
}

val countdownLabelBlock: @Composable () -> Unit = {
if (countdown != null) {
Text(
text = langContext.getString(
R.string.widget_countdown_label,
labels[countdown.first] ?: countdown.first,
snapshot.cityName
),
style = TextStyle(color = goldText, fontSize = 11.sp, fontWeight = FontWeight.Bold),
maxLines = 2,
textAlign = TextAlign.Center
)
}
}

val jalaliBlock: @Composable () -> Unit = {
Text(
text = snapshot.jalaliText,
style = TextStyle(color = goldText, fontSize = 12.sp, fontWeight = FontWeight.Bold),
maxLines = 1,
textAlign = TextAlign.Center
)
}

// -------------------------------
// خط ۱: ساعت
// -------------------------------
clockBlock()
Spacer(modifier = GlanceModifier.height(6.dp))

// -------------------------------
// خط ۲: شمارش معکوس
// -------------------------------
countdownLabelBlock()
Spacer(modifier = GlanceModifier.height(6.dp))

// -------------------------------
// خط ۳: تاریخ جلالی
// -------------------------------
jalaliBlock()
Spacer(modifier = GlanceModifier.height(6.dp))

// -------------------------------
// خط ۴: قمری | میلادی
// -------------------------------
Row(
modifier = GlanceModifier.fillMaxWidth(),
verticalAlignment = Alignment.Vertical.CenterVertically,
horizontalAlignment = Alignment.Horizontal.CenterHorizontally
) {

Text(
text = snapshot.hijriText,
style = TextStyle(color = faintGoldText, fontSize = 11.sp)
)

Spacer(modifier = GlanceModifier.width(6.dp))

Text(
text = "|",
style = TextStyle(color = faintGoldText, fontSize = 11.sp)
)

Spacer(modifier = GlanceModifier.width(6.dp))

Text(
text = gregorianText,
style = TextStyle(color = faintGoldText, fontSize = 11.sp)
)
}

Spacer(modifier = GlanceModifier.height(10.dp))

// -------------------------------
// خط ۵: جدول اوقات شرعی
// -------------------------------
Row(
modifier = GlanceModifier.fillMaxWidth(),
horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
verticalAlignment = Alignment.Vertical.CenterVertically
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

private fun formatGregorian(context: Context, dateKey: String): String {
return try {
val parts = dateKey.split("-").map { it.toInt() }
val cal = Calendar.getInstance().apply { set(parts[0], parts[1] - 1, parts[2]) }
SimpleDateFormat("d MMMM yyyy", context.resources.configuration.locales[0]).format(cal.time)
} catch (e: Exception) {
dateKey
}
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
valbackground(cellFillColor)
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

private fun formatGregorian(context: Context, dateKey: String): String {
return try {
val parts = dateKey.split("-").map { it.toInt() }
val cal = Calendar.getInstance().apply { set(parts[0], parts[1] - 1, parts[2]) }
SimpleDateFormat("d MMMM yyyy", context.resources.configuration.locales[0]).format(cal.time)
} catch (e: Exception) {
dateKey
}
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
val target = bestCal ?: return null
return bestKey to target.timeInMillis
}
