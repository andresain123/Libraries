// A standalone gallery for :orbs, mirroring the upstream iOS demo.
//
// This is a deliberate one-to-one port of ports/ios/ThinkingOrbsDemo's
// DemoApp.swift -- same chat-pill layout, same theme toggle, same speed
// control, same greys -- so the two can be screenshotted side by side and
// any divergence is the library's, not the demo's.
//
// The golden tests prove the animation math is byte-for-byte correct against
// the reference implementation, but they never touch a Canvas. This module
// exists to answer that on its own: no app startup, no DI, no flavours,
// nothing but :orbs and Compose. If an orb is blank here, the library is
// broken; if it renders here but not inside a host app, the wiring around it
// is.
//
// No material3 on purpose -- foundation only, so the module lifts upstream
// unchanged. That is why the speed slider below is hand-rolled rather than
// androidx.compose.material3.Slider.

package com.jakubantalik.thinkingorbs.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.jakubantalik.thinkingorbs.OrbSize
import com.jakubantalik.thinkingorbs.OrbState
import com.jakubantalik.thinkingorbs.OrbTheme
import com.jakubantalik.thinkingorbs.ThinkingOrb
import java.util.Locale

// SwiftUI's Color(white:) is sRGB grey, so each of these is the 8-bit
// rounding of the iOS demo's literal: 0.04 -> 0x0A, 0.97 -> 0xF7, and so on.
private val DarkBg = Color(0xFF0A0A0A)
private val LightBg = Color(0xFFF7F7F7)
private val DarkPill = Color(0xFF1A1A1A)
private val LightPill = Color.White
private val DarkTitle = Color(0xFFEBEBEB)
private val LightTitle = Color(0xFF1A1A1A)
private val DarkLabel = Color(0xFFE0E0E0)
private val LightLabel = Color(0xFF1F1F1F)
private val DarkSub = Color(0xFF737373)
private val LightSub = Color(0xFF8C8C8C)
private val DarkControl = Color(0xFF8C8C8C)
private val LightControl = Color(0xFF666666)
private val DarkTrack = Color(0xFF3A3A3A)
private val LightTrack = Color(0xFFD4D4D4)
private val Tint = Color(0xFF0A84FF)

// The ink path :app actually ships: GoilLoadingOrb always renders tinted, and
// the golden vectors cover the grey branch only. Without a tinted cell here
// nothing draws orbInk's tint branch on a Canvas anywhere, so a regression in
// 100% of production rendering would ship unseen.
private val DEMO_TINT = Color(0xFFD64040)

private const val SPEED_MIN = 0.25f
private const val SPEED_MAX = 3f
private val THUMB_RADIUS = 10.dp

class OrbGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge is only mandatory from API 35. Enabling it explicitly
        // means the root Column's background really does paint under the bars
        // on older levels too -- otherwise the status bar keeps the theme's
        // opaque colorPrimaryDark and light-mode glyphs land on black.
        enableEdgeToEdge()
        setContent {
            var dark by rememberSaveable { mutableStateOf(true) }
            // SwiftUI gets this from .preferredColorScheme; on Android the
            // system bars are the activity's problem, and without this the
            // status bar keeps light-on-light glyphs after a toggle to light.
            LaunchedEffect(dark) { applySystemBars(dark) }
            OrbDemo(dark = dark, onToggleTheme = { dark = !dark })
        }
    }

    // Only the glyph appearance: the bar backgrounds come from the root Column,
    // which paints under them once edge-to-edge is on. Both bars, because in
    // light mode the navigation glyphs are otherwise white on near-white.
    private fun applySystemBars(dark: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

@Composable
private fun OrbDemo(dark: Boolean, onToggleTheme: () -> Unit) {
    // Saveable: the module exists for side-by-side screenshot comparison, and
    // losing the chosen theme and speed on every rotation defeats that.
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    val theme = if (dark) OrbTheme.Dark else OrbTheme.Light

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) DarkBg else LightBg)
            // targetSdk 35+ makes edge-to-edge mandatory, so without this the
            // header sits under the status bar and the navigation bar lands on
            // top of the speed slider. SwiftUI's safe area is implicit; here it
            // is not. Background first, padding second, so the colour still
            // runs under the bars.
            .safeDrawingPadding(),
    ) {
        Header(dark = dark, onToggleTheme = onToggleTheme)

        // Lazy, not a scrolling Column: every ThinkingOrb holds its own
        // withFrameNanos loop and rebuilds an OrbFrame per frame, so composing
        // all of them eagerly keeps ~18 clocks running for pills that are off
        // screen. LazyColumn disposes the ones scrolled away.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(OrbState.entries) { state ->
                Pill(state = state, dark = dark, theme = theme, speed = speed)
            }
            item {
                Pill(
                    state = OrbState.Searching,
                    dark = dark,
                    theme = theme,
                    speed = speed,
                    tint = DEMO_TINT,
                )
            }
        }

        Controls(dark = dark, speed = speed, onSpeed = { speed = it })
    }
}

@Composable
private fun Header(dark: Boolean, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "Thinking Orbs",
            style = TextStyle(
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dark) DarkTitle else LightTitle,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        BasicText(
            text = if (dark) "Light" else "Dark",
            modifier = Modifier.clickable(onClick = onToggleTheme),
            style = TextStyle(
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = if (dark) DarkTitle else LightTitle,
            ),
        )
    }
}

@Composable
private fun Pill(
    state: OrbState,
    dark: Boolean,
    theme: OrbTheme,
    speed: Float,
    tint: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (dark) DarkPill else LightPill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThinkingOrb(
            state = state,
            size = OrbSize.Px64,
            theme = theme,
            speed = speed.toDouble(),
            tint = tint,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = if (tint != null) "Agent ${state.id}… (tinted)" else "Agent ${state.id}…",
                style = TextStyle(
                    fontSize = 15.sp,
                    color = if (dark) DarkLabel else LightLabel,
                ),
            )
            BasicText(
                text = state.id,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (dark) DarkSub else LightSub,
                ),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // the 20px inline size, at its own tuning -- not a scaled-down Px64
        ThinkingOrb(
            modifier = Modifier.padding(end = 6.dp),
            state = state,
            size = OrbSize.Px20,
            theme = theme,
            speed = speed.toDouble(),
            tint = tint,
        )
    }
}

@Composable
private fun Controls(dark: Boolean, speed: Float, onSpeed: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val label = TextStyle(
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = if (dark) DarkControl else LightControl,
        )
        BasicText(text = "SPEED", style = label)
        SpeedSlider(
            modifier = Modifier.weight(1f),
            value = speed,
            onValue = onSpeed,
            dark = dark,
        )
        BasicText(
            text = String.format(Locale.US, "%.2f×", speed),
            modifier = Modifier.width(48.dp),
            style = label.copy(textAlign = TextAlign.End),
        )
    }
}

/**
 * Minimal replacement for SwiftUI's `Slider`, so the demo keeps its
 * foundation-only dependency set. Track and thumb only — no ripple, no
 * accessibility semantics beyond the drag itself.
 */
@Composable
private fun SpeedSlider(
    modifier: Modifier,
    value: Float,
    onValue: (Float) -> Unit,
    dark: Boolean,
) {
    val fraction = ((value - SPEED_MIN) / (SPEED_MAX - SPEED_MIN)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(28.dp)
            // One gesture loop rather than a tap detector stacked on a drag
            // detector: a real slider commits on press, where two detectors
            // leave press-and-hold doing nothing until slop is crossed or the
            // finger lifts, with the consumption order between them implicit.
            .pointerInput(Unit) {
                val inset = THUMB_RADIUS.toPx()
                val width = { size.width.toFloat() }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onValue(valueAt(down.position.x, width(), inset))
                    horizontalDrag(down.id) { change ->
                        onValue(valueAt(change.position.x, width(), inset))
                        change.consume()
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thumbR = THUMB_RADIUS.toPx()
            val trackH = 4.dp.toPx()
            val cy = size.height / 2f
            val radius = CornerRadius(trackH / 2f)
            // The thumb has to stay inside the box at both extremes, so the
            // travel is inset by its radius at each end.
            val travel = size.width - thumbR * 2
            val cx = thumbR + travel * fraction

            drawRoundRect(
                color = if (dark) DarkTrack else LightTrack,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = Tint,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(cx, trackH),
                cornerRadius = radius,
            )
            drawCircle(color = Color.White, radius = thumbR, center = Offset(cx, cy))
        }
    }
}

/**
 * Maps a touch x onto the speed range over the same inset travel the thumb is
 * drawn along, so the thumb lands under the finger instead of drifting from it
 * near either end.
 */
private fun valueAt(x: Float, widthPx: Float, inset: Float): Float {
    val travel = widthPx - inset * 2
    if (travel <= 0f) return SPEED_MIN
    val fraction = ((x - inset) / travel).coerceIn(0f, 1f)
    return SPEED_MIN + fraction * (SPEED_MAX - SPEED_MIN)
}
