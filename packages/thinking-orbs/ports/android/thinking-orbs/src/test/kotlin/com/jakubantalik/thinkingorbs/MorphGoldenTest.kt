package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphGoldenTest {

    @Test
    fun `morph matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("morph")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }

    /** Every morph dot sits on the view plane; a stable sort is what keeps it ordered. */
    @Test
    fun `morph emits a flat outline`() {
        val frame = orbFrame(OrbState.Shaping, OrbSize.Px64, t = 1.7)
        assertTrue(frame.dots.isNotEmpty())
        for (d in frame.dots) assertEquals(0.0, d.z, 0.0)
    }
}
