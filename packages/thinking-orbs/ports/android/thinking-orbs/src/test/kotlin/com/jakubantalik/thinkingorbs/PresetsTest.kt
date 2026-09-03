package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PresetsTest {

    /**
     * Every resolved (state, size) pair must match the reference exactly. This
     * is what catches a wrong rounding mode: three of these presets sit on an
     * exact .5 tie.
     */
    @Test
    fun `resolves every preset exactly as the reference does`() {
        val golden = GoldenFixture.load()

        for ((key, expected) in golden.resolved) {
            val (stateId, sizePx) = key.split("-")
            val state = requireNotNull(OrbState.fromId(stateId)) { "unknown state in $key" }
            val size = requireNotNull(OrbSize.fromPx(sizePx.toInt())) { "unknown size in $key" }

            val actual = resolvePreset(state, size)

            assertEquals("$key mode", expected.mode, actual.mode.id)
            assertEquals("$key speed", expected.speed, actual.speed, 0.0)
            assertEquals("$key opts keys", expected.opts.keys, actual.opts.keys)
            for ((k, v) in expected.opts) {
                assertEquals("$key opts.$k", v, actual.opts.getValue(k), 1e-12)
            }
        }
    }

    /**
     * Regression guards for the exact ties. kotlin.math.round would return
     * 40.0, 2.0 and 2.0 here and every one of them changes the geometry.
     */
    @Test
    fun `tie-breaking presets round toward positive infinity`() {
        assertEquals(
            41.0,
            resolvePreset(OrbState.Connecting, OrbSize.Px64).opts.getValue("nodeN"),
            0.0,
        )
        assertEquals(
            3.0,
            resolvePreset(OrbState.Composing, OrbSize.Px64).opts.getValue("lanes"),
            0.0,
        )
        assertEquals(
            3.0,
            resolvePreset(OrbState.Breathing, OrbSize.Px64).opts.getValue("lanes"),
            0.0,
        )
    }

    /** An explicit 0 means the mode opted out of a layer; scaling must not resurrect it. */
    @Test
    fun `a zero count stays zero after scaling`() {
        assertEquals(
            0.0,
            resolvePreset(OrbState.Breathing, OrbSize.Px64).opts.getValue("ghostN"),
            0.0,
        )
    }

    @Test
    fun `resolution is cached and returns the same instance`() {
        val a = resolvePreset(OrbState.Searching, OrbSize.Px64)
        val b = resolvePreset(OrbState.Searching, OrbSize.Px64)
        assertSame(a, b)
    }
}
