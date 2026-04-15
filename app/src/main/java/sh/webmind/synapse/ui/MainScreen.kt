package sh.webmind.synapse.ui

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import sh.webmind.synapse.NodeViewModel
import sh.webmind.synapse.service.NodeService
import sh.webmind.synapse.ui.theme.*

/**
 * Fullscreen WebView hosting https://synapse.webmind.sh/ — same UI as the
 * website so there's only one surface to maintain. All chrome (status strip,
 * chat, Contribute toggle, log drawer) lives in the web page.
 *
 * Native stays minimal:
 *  - Starts the foreground service on app launch so GPU compute keeps
 *    running when the user backgrounds the app (combined with game-mode +
 *    mediaPlayback FG type per v1.0.2 manifest).
 *  - Thin update-available banner overlay, since in-app updates are a
 *    Kotlin-only feature not reachable from inside the WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen(viewModel: NodeViewModel) {
    val coordUrl by viewModel.coordinatorUrl.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val updateDownloading by viewModel.updateDownloading.collectAsState()
    val ctx = LocalContext.current

    // Start the foreground service as soon as the app opens so GPU
    // compute keeps running when backgrounded. Stop it when the composable
    // is removed — in practice that only happens when the user force-
    // closes the app.
    DisposableEffect(Unit) {
        NodeService.start(ctx)
        onDispose { /* let the service outlive the Activity; user stops via notification */ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Fullscreen WebView — the unified Synapse page IS the app.
        SynapseWebView(
            url = coordUrl,
            modifier = Modifier.fillMaxSize(),
            onTokens = { viewModel.recordTokens(it) },
            onRequest = { viewModel.recordRequest() },
        )

        // Update banner overlay — slides in at top when a new release is
        // available on GitHub. Native Compose because in-app install needs
        // Android APIs unreachable from the WebView.
        availableUpdate?.let { release ->
            Surface(
                color = Accent.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${release.tagName} available",
                            fontFamily = Mono, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = BgPrimary,
                        )
                        Text(
                            if (updateDownloading) "downloading…" else "tap update to install",
                            fontFamily = Mono, fontSize = 10.sp, color = BgPrimary,
                        )
                    }
                    TextButton(onClick = { viewModel.installUpdate() }) {
                        Text("update", fontFamily = Mono, fontSize = 12.sp,
                             fontWeight = FontWeight.Bold, color = BgPrimary)
                    }
                    TextButton(onClick = { viewModel.dismissUpdate() }) {
                        Text("×", fontFamily = Mono, fontSize = 14.sp, color = BgPrimary)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SynapseWebView(
    url: String,
    modifier: Modifier = Modifier,
    onTokens: (Long) -> Unit,
    onRequest: () -> Unit,
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                // Critical for background operation: keeps WebView rasterizing
                // even when it's offscreen or the activity is backgrounded.
                if (androidx.webkit.WebViewFeature.isFeatureSupported(
                        androidx.webkit.WebViewFeature.OFF_SCREEN_PRERASTER)) {
                    androidx.webkit.WebSettingsCompat.setOffscreenPreRaster(settings, true)
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                // Don't pause timers when the activity is paused/stopped.
                resumeTimers()

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    // Load all URLs inside the WebView (don't kick out to browser).
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                }

                // JS bridge so the page can report compute stats back to native.
                addJavascriptInterface(object {
                    @JavascriptInterface fun tokens(count: Int) { onTokens(count.toLong()) }
                    @JavascriptInterface fun request() { onRequest() }
                }, "SynapseBridge")

                loadUrl(url)
            }
        },
        update = { wv ->
            // Re-resume timers each recomposition — defensive against
            // implicit onPause from Compose lifecycle transitions.
            wv.resumeTimers()
        },
        modifier = modifier,
    )
}
