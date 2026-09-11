package com.example.physi_lock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.example.physi_lock.data.context.lastKnownLocation
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono

/** Real map, via Leaflet.js in a WebView -- per the user's explicit ask (2026-09-04), not
 *  a native Compose/Play-Services-Maps widget. Map *tiles* still come from OpenStreetMap's
 *  public tile server live (needs the INTERNET permission this app already has) -- that's
 *  unavoidable, it's live imagery. Leaflet's own JS/CSS/marker-icon assets are bundled
 *  locally under `assets/leaflet/` (2026-09-10 fix), not fetched from unpkg.com at runtime.
 *
 *  2026-09-10, THIRD fix -- the actual root cause, confirmed via chrome://inspect on-device:
 *  serving the bundled page via `loadUrl("file:///android_asset/...")` hit a real Chromium
 *  WebView limitation, not a code bug -- `file://` URLs are treated as unique/opaque origins,
 *  and reloading the exact same `file://` URL (e.g. navigating away from this screen and
 *  back, which recreates the WebView) gets blocked outright: "Unsafe attempt to load URL
 *  file:///android_asset/leaflet/map.html from frame with URL
 *  file:///android_asset/leaflet/map.html. 'file:' URLs are treated as unique security
 *  origins" / Network tab showed `map.html` as `blocked:origin`, zero tile requests ever
 *  fired. Fixed the correct way Google recommends for this exact scenario: `WebViewAssetLoader`
 *  (androidx.webkit) serves the same bundled assets over a real, stable
 *  `https://appassets.androidplatform.net/assets/...` origin instead of `file://` --
 *  reloads/repeat navigation behave like any normal https page, no unique-origin conflict.
 *
 *  2026-09-10, FOURTH fix -- the actual remaining root cause after the above, confirmed via
 *  chrome://inspect's Elements panel: `#map` measured 333x**0** (real width, zero height)
 *  even though the native WebView itself was correctly sized. `height:100%` on `#map`
 *  depends on the whole html/body ancestor chain resolving a definite height, which wasn't
 *  settling reliably inside this WebView. Fixed in `map.html`: `#map` uses
 *  `position:absolute; top/left/right/bottom:0` instead of percentage height (resolves
 *  against the containing block directly, not a percentage chain), plus a
 *  `map.invalidateSize()` safety-net call shortly after creation in case sizing settles
 *  asynchronously. (Two earlier speculative fixes -- forcing `LAYER_TYPE_SOFTWARE` and a
 *  near-1.0 `alpha` on the AndroidView, both common fixes for a *different* class of
 *  Compose-doesn't-composite-native-views bug -- were tried and ruled out before this was
 *  found; removed again since they did nothing here.)
 *  Tapping the map drops a pin + shows the alert radius, and calls back into Kotlin via a
 *  JavaScript interface. No location SDK dependency added -- the "use my current location"
 *  fallback center reuses the same plain-LocationManager
 *  [com.example.physi_lock.data.context.lastKnownLocation] read the background proximity
 *  check itself uses. Extracted 2026-09-09 from ContextAlertsScreen.kt (now LocationScreen.kt)
 *  so School and Work can each hold their own independent map/pin instance. */
@Composable
fun LeafletMapPicker(
    initialLat: Double?,
    initialLng: Double?,
    radiusMeters: Int,
    onPick: (Double, Double) -> Unit
) {
    val context = LocalContext.current

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(15.dp)),
        factory = { ctx ->
            val fallback = lastKnownLocation(ctx)
            val startLat = initialLat ?: fallback?.latitude ?: 0.0
            val startLng = initialLng ?: fallback?.longitude ?: 0.0
            val startZoom = if (initialLat != null) 16 else if (fallback != null) 13 else 2
            val hasMarker = initialLat != null && initialLng != null

            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            android.webkit.WebView.setWebContentsDebuggingEnabled(true)
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: android.webkit.WebView,
                        request: android.webkit.WebResourceRequest
                    ): android.webkit.WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.d("LeafletMapPicker", "onPageFinished: $url -- calling initMap")
                        view?.evaluateJavascript(
                            "if (window.initMap) { window.initMap($startLat, $startLng, $startZoom, $radiusMeters, $hasMarker, $startLat, $startLng); } " +
                                "else { console.error('window.initMap not defined -- map.html failed to run'); }",
                            null
                        )
                    }
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                        android.util.Log.d(
                            "LeafletMapPicker",
                            "JS console: ${message.message()} (${message.sourceId()}:${message.lineNumber()})"
                        )
                        return true
                    }
                }
                addJavascriptInterface(
                    object {
                        @android.webkit.JavascriptInterface
                        fun onPick(lat: Double, lng: Double) {
                            post { onPick(lat, lng) }
                        }
                    },
                    "AndroidBridge"
                )
                // This card sits inside LocationScreen's verticalScroll Column -- without
                // this, dragging to pan the map also scrolls the whole screen, since Compose's
                // scrollable modifier doesn't automatically defer to a touch a nested native
                // View (the WebView) is already handling. Standard fix: tell the parent to
                // stop intercepting touches for the duration of a touch sequence that started
                // on the map, same pattern used for GoogleMap/WebView inside a ScrollView.
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        android.view.MotionEvent.ACTION_UP -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            v.performClick()
                        }
                        android.view.MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
                loadUrl("https://appassets.androidplatform.net/assets/leaflet/map.html")
            }
        },
        update = { view -> view.evaluateJavascript("if (window.setRadius) { window.setRadius($radiusMeters); }", null) },
        onRelease = { view -> view.destroy() }
    )
}

@Composable
fun RadiusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .background(if (selected) DeepOlive else DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontFamily = DmMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) BackgroundLight else DeepOlive
        )
    }
}
