package sh.webmind.synapse.ui

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.webmind.synapse.NodeViewModel
import sh.webmind.synapse.data.Badges
import sh.webmind.synapse.ui.theme.*

@Composable
fun MainScreen(viewModel: NodeViewModel) {
    val stats by viewModel.stats.collectAsState()
    val contributing by viewModel.contributing.collectAsState()
    val chargingOnly by viewModel.chargingOnly.collectAsState()
    val coordUrl by viewModel.coordinatorUrl.collectAsState()

    val scope = rememberCoroutineScope()

    // Periodic active-seconds ticker when contributing
    LaunchedEffect(contributing) {
        if (contributing) {
            while (true) {
                delay(10_000)
                viewModel.addActiveSeconds(10)
            }
        }
    }

    // Battery guard
    LaunchedEffect(contributing) {
        while (true) {
            delay(30_000)
            if (contributing.not()) { delay(60_000); continue }
            val level = viewModel.batteryLevel()
            if (level in 1..19) {
                viewModel.toggleContributing() // auto-stop at low battery
            } else if (chargingOnly && !viewModel.isCharging()) {
                viewModel.toggleContributing()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingNode(active = contributing)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Synapse", fontFamily = Mono, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    if (contributing) "your phone is thinking"
                    else "tap to contribute compute",
                    fontFamily = Mono, fontSize = 12.sp, color = TextSecondary
                )
            }
            // Handle badge
            Text(
                stats.handle,
                fontFamily = Mono, fontSize = 10.sp,
                color = AccentLight,
                modifier = Modifier
                    .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        // Big toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { viewModel.toggleContributing() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (contributing) Accent else BgElevated,
                    contentColor = if (contributing) BgPrimary else TextPrimary
                )
            ) {
                Text(
                    if (contributing) "● contributing  tap to stop"
                    else "○ start contributing",
                    fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("tokens", formatNumber(stats.tokensProcessed), Accent, Modifier.weight(1f))
            StatCard("requests", formatNumber(stats.requestsHandled), Cyan, Modifier.weight(1f))
            StatCard("uptime", formatDuration(stats.activeSeconds), Violet, Modifier.weight(1f))
        }

        // Settings card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .background(BgSurface, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Text("settings", fontFamily = Mono, fontSize = 11.sp, color = TextSubtle)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("only when charging", fontFamily = Mono, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = chargingOnly,
                    onCheckedChange = { viewModel.setChargingOnly(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Accent, checkedTrackColor = Accent.copy(alpha = 0.3f)
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "auto-stops below 20% battery",
                fontFamily = Mono, fontSize = 11.sp, color = TextSubtle
            )
        }

        // Badges
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "badges — ${stats.badges.size}/${Badges.ALL.size}",
                fontFamily = Mono, fontSize = 11.sp, color = TextSubtle,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Badges.ALL) { badge ->
                    val earned = badge.id in stats.badges
                    BadgeChip(badge.emoji, badge.title, earned)
                }
            }
        }

        // Status message
        if (contributing) {
            Text(
                when {
                    stats.tokensProcessed == 0L -> "connecting to the mesh…"
                    stats.tokensProcessed < 100 -> "warming up"
                    stats.tokensProcessed < 1000 -> "in the flow"
                    else -> "quietly thinking with the world"
                },
                fontFamily = Mono, fontSize = 12.sp, color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }

        // Hidden WebView — runs the actual Synapse node logic
        if (contributing) {
            SynapseWebView(
                url = coordUrl,
                onTokens = { viewModel.recordTokens(it) },
                onRequest = { viewModel.recordRequest() }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Footer
        Text(
            "ai-generated app — built by Claude, idea by Tejas",
            fontFamily = Mono, fontSize = 9.sp, color = TextSubtle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

@Composable
private fun PulsingNode(active: Boolean) {
    val infinite = rememberInfiniteTransition(label = "node")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.3f, targetValue = if (active) 1f else 0.3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((28 * scale).dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = glow * 0.2f))
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (active) Accent else TextSubtle)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AccentLight)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, mod: Modifier = Modifier) {
    Column(
        modifier = mod
            .background(BgSurface, RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Text(label, fontFamily = Mono, fontSize = 10.sp, color = TextSubtle)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun BadgeChip(emoji: String, title: String, earned: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(BgSurface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .width(80.dp)
    ) {
        Text(
            if (earned) emoji else "·",
            fontSize = 22.sp,
            modifier = Modifier.alpha(if (earned) 1f else 0.3f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            title, fontFamily = Mono, fontSize = 9.sp,
            color = if (earned) TextPrimary else TextSubtle,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SynapseWebView(url: String, onTokens: (Long) -> Unit, onRequest: () -> Unit) {
    val ctx = LocalContext.current
    AndroidView(
        factory = {
            WebView(it).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                }

                // Bridge so node JS can report back
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun tokens(count: Int) { onTokens(count.toLong()) }

                    @android.webkit.JavascriptInterface
                    fun request() { onRequest() }
                }, "SynapseBridge")

                loadUrl(url)
                layoutParams = android.view.ViewGroup.LayoutParams(1, 1) // hidden
            }
        },
        modifier = Modifier.size(1.dp)
    )
}

// Formatters
private fun formatNumber(n: Long): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}.${(n % 1_000_000) / 100_000}M"
    n >= 1_000 -> "${n / 1_000}.${(n % 1_000) / 100}k"
    else -> n.toString()
}

private fun formatDuration(seconds: Long): String = when {
    seconds >= 86_400 -> "${seconds / 86_400}d"
    seconds >= 3_600 -> "${seconds / 3_600}h"
    seconds >= 60 -> "${seconds / 60}m"
    else -> "${seconds}s"
}

