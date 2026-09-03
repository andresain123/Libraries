package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebGoldenTest {

    @Test
    fun `web matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("web")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }

    @Test
    fun `web is the mode that emits lines`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("web")
        assertTrue("expected some web cases to carry lines", cases.any { it.lineCount > 0 })
    }

    /** The tie-rounded node count from Task 4, observed through real geometry. */
    @Test
    fun `connecting at 64 builds 41 nodes`() {
        val preset = resolvePreset(OrbState.Connecting, OrbSize.Px64)
        assertEquals(41.0, preset.opts.getValue("nodeN"), 0.0)
    }
}
