package com.jakubantalik.thinkingorbs

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrozenTimeTest {

    /**
     * Regression guard for the bug upstream's iOS port shipped: a "freeze time"
     * hook that multiplied the frozen instant by the preset speed. The golden
     * vectors evaluate the engine at raw t, so the view and the vectors must
     * agree on which instant they mean.
     */
    @Test
    fun `a frozen instant evaluates the engine at raw t`() {
        val golden = GoldenFixture.load()
        val case = golden.cases.first { it.state == "searching" && it.size == 64 && it.t == 1.7 }

        val preset = resolvePreset(OrbState.Searching, OrbSize.Px64)
        // Kotlin's `assert` is a no-op unless the JVM runs with -ea, so use a
        // real assertion: this precondition is what gives the test its teeth.
        assertNotEquals("preset speed must not be 1, or this test proves nothing",
            1.0, preset.speed, 1e-9)

        val frame = orbFrame(preset, 64.0, case.t)
        assertEquals(case.dotCount, frame.dots.size)

        val scaled = orbFrame(preset, 64.0, case.t * preset.speed)
        assertTrue(
            "scaling frozen time by speed must produce a DIFFERENT frame",
            scaled.dots.zip(frame.dots).any { (a, b) -> a.x != b.x || a.y != b.y },
        )
    }

    @Test
    fun `ink mirrors on dark themes and quantises to 8 bits`() {
        // white = 0 is darkest ink on paper: black on light, white on dark
        assertEquals(0f, orbInk(0.0, 1.0, isDark = false, tint = null).red, 1e-6f)
        assertEquals(1f, orbInk(0.0, 1.0, isDark = true, tint = null).red, 1e-6f)
        // out-of-range input is clamped, not wrapped
        assertEquals(1f, orbInk(2.0, 1.0, isDark = false, tint = null).red, 1e-6f)
        assertEquals(0f, orbInk(-1.0, 1.0, isDark = false, tint = null).red, 1e-6f)
    }

    /**
     * The regression guard for upstream's own bug. If someone scales the frozen
     * instant by effSpeed, this fails.
     */
    @Test
    fun `a frozen instant is returned raw, whatever else is true`() {
        assertEquals(1.7, resolveInstant(1.7, clockSeconds = 99.0, animate = false, reduceMotion = true), 0.0)
        assertEquals(1.7, resolveInstant(1.7, clockSeconds = 99.0, animate = true, reduceMotion = false), 0.0)
        assertEquals(1.7, resolveInstant(1.7, clockSeconds = 99.0, animate = false, reduceMotion = false), 0.0)
        // and it must not be the clock value either
        assertNotEquals(99.0, resolveInstant(1.7, 99.0, animate = true, reduceMotion = false), 1e-9)
    }

    @Test
    fun `the animated path uses the clock and reduced motion uses raw engine time`() {
        assertEquals(
            42.0,
            resolveInstant(null, clockSeconds = 42.0, animate = true, reduceMotion = false),
            0.0,
        )
        assertEquals(
            OrbSpec.REDUCED_MOTION_T,
            resolveInstant(null, clockSeconds = 42.0, animate = false, reduceMotion = true),
            1e-12,
        )
    }

    /**
     * Pausing holds the pose the orb was already showing. Returning the
     * reduced-motion instant instead would make it jump to an unrelated frame
     * on pause, and jump again on resume — the clock keeps running either way.
     */
    @Test
    fun `pausing freezes the current instant rather than snapping to reduced motion`() {
        assertEquals(
            42.0,
            resolveInstant(null, clockSeconds = 42.0, animate = false, reduceMotion = false),
            0.0,
        )
        assertNotEquals(
            OrbSpec.REDUCED_MOTION_T,
            resolveInstant(null, clockSeconds = 42.0, animate = false, reduceMotion = false),
            1e-9,
        )
    }

    /**
     * The static instant is raw engine time, so it must NOT vary with speed.
     * spec.paint.reducedMotion says "a single static frame at t = 0.6" and
     * spec.paint.clock defines t as already-scaled engine time.
     */
    @Test
    fun `the reduced-motion instant is raw engine time, whatever the clock reads`() {
        val early = resolveInstant(null, 0.0, animate = false, reduceMotion = true)
        val late = resolveInstant(null, 86_400.0, animate = false, reduceMotion = true)
        assertEquals(OrbSpec.REDUCED_MOTION_T, early, 1e-12)
        assertEquals(OrbSpec.REDUCED_MOTION_T, late, 1e-12)
        assertEquals(early, late, 0.0)
    }

    /**
     * The tinted branch is the only ink path the consuming app renders, and it
     * has no golden vector — the fixture carries greys only. So this pins it to
     * inkColor() in src/engine/core.ts instead: the ramp runs on the tint's own
     * channels, toward white on a light substrate and toward black on a dark
     * one, with alpha passed straight through.
     *
     * Expected values are the 8-bit results the web's Math.round produces, not
     * the raw reals, because Color stores sRGB at 8 bits per channel and would
     * quantise them anyway.
     */
    @Test
    fun `the tinted ink ramps the hue with depth, per theme`() {
        val tint = Color(0xFFD64040)   // 214, 64, 64

        // w = 0 is the near face: the tint itself, both themes.
        for (dark in listOf(false, true)) {
            val near = orbInk(white = 0.0, alpha = 1.0, isDark = dark, tint = tint)
            assertEquals(214f / 255f, near.red, 1e-6f)
            assertEquals(64f / 255f, near.green, 1e-6f)
            assertEquals(64f / 255f, near.blue, 1e-6f)
            assertEquals(1.0f, near.alpha, 1e-6f)
        }

        // Light: c + (255 - c) * w, so the far face washes out to white.
        val lightMid = orbInk(0.5, 1.0, isDark = false, tint = tint)
        assertEquals(235f / 255f, lightMid.red, 1e-6f)     // 214 + 41*0.5 = 234.5 -> 235
        assertEquals(160f / 255f, lightMid.green, 1e-6f)   // 64 + 191*0.5 = 159.5 -> 160
        val lightFar = orbInk(1.0, 1.0, isDark = false, tint = tint)
        assertEquals(1f, lightFar.red, 1e-6f)
        assertEquals(1f, lightFar.blue, 1e-6f)

        // Dark: c * (1 - w), so the far face sinks to black.
        val darkMid = orbInk(0.5, 1.0, isDark = true, tint = tint)
        assertEquals(107f / 255f, darkMid.red, 1e-6f)      // 214*0.5 = 107
        assertEquals(32f / 255f, darkMid.green, 1e-6f)     // 64*0.5  = 32
        val darkFar = orbInk(1.0, 1.0, isDark = true, tint = tint)
        assertEquals(0f, darkFar.red, 1e-6f)
        assertEquals(0f, darkFar.blue, 1e-6f)

        // Alpha is the dot's own, never folded into depth. 128/255, not 0.5:
        // Color stores sRGB at 8 bits per channel, alpha included.
        assertEquals(128f / 255f, orbInk(0.7, 0.5, isDark = false, tint = tint).alpha, 1e-6f)

        // Theme now DOES change a tinted result -- it did not before.
        assertNotEquals(
            orbInk(0.4, 0.8, isDark = false, tint = tint),
            orbInk(0.4, 0.8, isDark = true, tint = tint),
        )
    }
}
