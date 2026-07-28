package com.qibla.prayertimes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qibla.prayertimes.R
import com.qibla.prayertimes.data.PrayerTimesState
import com.qibla.prayertimes.data.prayerLabels
import com.qibla.prayertimes.ui.theme.*
import com.qibla.prayertimes.util.HijriCalendar
import com.qibla.prayertimes.util.JalaliCalendar
import com.qibla.prayertimes.viewmodel.QiblaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The salawat formula is always recited in Arabic regardless of the app's UI language, so it
// is intentionally not routed through strings.xml (which would risk it being translated).
private const val SALAWAT_TEXT =
    "اَللّهُمَّ صَلِّ عَلَی مُحَمَّدِِ وَ آلِ مُحَمَّد وَ عَجِّل فَرَجَهُم وَ العَن اَعدائَهُم اَجمعین"

// The "Entezar" font, used for the salawat text.
private val salawatFontFamily = FontFamily(Font(R.font.entezar))

private val HOME_PRAYER_KEYS = listOf("Fajr", "Sunrise", "Dhuhr", "Sunset", "Maghrib", "Midnight")

@Composable
fun QiblaScreen(
    viewModel: QiblaViewModel,
    onOpenMenu: () -> Unit,
    onOpenCityPicker: () -> Unit
) {
    val selected by viewModel.selectedCity.collectAsState()
    val prayerState by viewModel.prayerState.collectAsState()
    val context = LocalContext.current
    val labels = prayerLabels(context)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(NightSlate, NightMid, NightDeep),
                    radius = 1400f
                )
            )
    ) {
        val isCompact = maxWidth < 360.dp
        val dialSize = if (isCompact) (maxWidth * 0.62f).coerceAtMost(220.dp) else 260.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isCompact) 14.dp else 20.dp)
                .padding(top = 20.dp, bottom = 32.dp)
        ) {
            // Top bar: city (with pin, opens city picker) on one side, menu on the other.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenCityPicker)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BrassLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(selected.name, color = AmberText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu), tint = AmberMuted)
                }
            }

            Spacer(Modifier.height(18.dp))

            // Salawat
            Text(
                text = SALAWAT_TEXT,
                color = AmberText,
                fontSize = if (isCompact) 15.sp else 17.sp,
                fontFamily = salawatFontFamily,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = if (isCompact) 24.sp else 27.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Gregorian + Jalali + Hijri date, all three always shown together.
            val gregorianText = remember { SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Date()) }
            val jalaliText = remember { JalaliCalendar.today().toString() }
            val hijriApproxText = remember {
                val h = HijriCalendar.today()
                val language = context.resources.configuration.locales[0].language
                val monthName = if (language == "ar" || language == "fa") h.monthNameAr else h.monthNameEn
                "${h.day} $monthName ${h.year}"
            }
            val hijriFromApi = (prayerState as? PrayerTimesState.Success)?.result?.hijri
            val eraSuffix = stringResource(R.string.hijri_era_suffix)
            val hijriText = hijriFromApi?.let { "${it.day} ${it.monthName(context)} ${it.year}$eraSuffix" } ?: hijriApproxText

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(gregorianText, color = AmberText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(1.dp))
                Text(jalaliText, color = AmberText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(1.dp))
                Text(hijriText, color = AmberText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(18.dp))

            // Prayer times (Fajr, Sunrise, Dhuhr, Sunset, Maghrib, Midnight)
            val timings = (prayerState as? PrayerTimesState.Success)?.result?.timings
            when {
                prayerState is PrayerTimesState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Brass, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                }
                timings != null -> {
                    val rows = HOME_PRAYER_KEYS.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowKeys ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                rowKeys.forEach { key ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(OverlayFaint)
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(labels[key] ?: key, color = AmberMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                                        Text(
                                            timings[key] ?: "--:--",
                                            color = AmberText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Compass, at the very bottom, with nothing after it.
            val bearing = viewModel.bearing.toFloat()
            val deviceHeading = com.qibla.prayertimes.sensor.rememberDeviceHeading()
            val needleAngle = if (deviceHeading != null) (bearing - deviceHeading + 360f) % 360f else bearing
            val dialRotation = if (deviceHeading != null) (360f - deviceHeading) % 360f else 0f
            val isAlignedWithQibla = deviceHeading != null &&
                minOf(needleAngle, 360f - needleAngle) < 6f
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CompassDial(
                    bearingDegrees = needleAngle,
                    dialSize = dialSize,
                    animationMillis = if (deviceHeading != null) 150 else 700,
                    centerLabel = "${"%.1f".format(bearing)}°",
                    captionText = if (deviceHeading != null)
                        stringResource(R.string.compass_live_caption)
                    else
                        stringResource(R.string.compass_static_caption),
                    dialRotationDegrees = dialRotation,
                    isAligned = isAlignedWithQibla
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.compass_distance, viewModel.distanceKm),
                color = AmberMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            val isOffline = (prayerState as? PrayerTimesState.Success)?.result?.isOffline == true
            if (isOffline) {
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.offline_explanation),
                    color = AmberFaint,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Manual refresh — always pinned to the literal left edge, regardless of RTL/LTR.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = { viewModel.refreshPrayerTimes() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_times), tint = AmberMuted)
                    }
                }
            }
        }
    }
}
