package com.qibla.prayertimes.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.qibla.prayertimes.R
import com.qibla.prayertimes.data.CityStore
import com.qibla.prayertimes.ui.theme.*

/**
 * Full-screen interactive map (OpenStreetMap tiles via Leaflet.js in a WebView) for picking a
 * location. The coordinate readout and the confirm button live inside the HTML page itself
 * (not as separate native Compose UI), mirroring a known-working implementation exactly.
 * This is an online-only screen — the offline equivalent is "enter coordinates manually" back
 * on the city picker screen.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapPickerScreen(onBack: () -> Unit, onPicked: (lat: Double, lon: Double) -> Unit) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val context = LocalContext.current
    val initialCity = remember { CityStore(context).loadSelectedCity() }

    Column(modifier = Modifier.fillMaxSize().background(NightMid)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = AmberText)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onLocationPicked(lat: Double, lng: Double) {
                                    mainHandler.post { onPicked(lat, lng) }
                                }
                            },
                            "AndroidBridge"
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                if (initialCity != null) {
                                    view.evaluateJavascript(
                                        "setInitialLocation(${initialCity.lat}, ${initialCity.lon})",
                                        null
                                    )
                                }
                            }
                        }
                        loadUrl("file:///android_asset/map_picker.html")
                    }
                }
            )
        }
    }
}
