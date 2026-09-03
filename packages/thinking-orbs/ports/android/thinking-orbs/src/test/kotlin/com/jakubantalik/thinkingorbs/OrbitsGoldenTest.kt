package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Test

class OrbitsGoldenTest {

    @Test
    fun `orbits matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("orbits")

        assertEquals("expected 8 orbits cases", 8, cases.size)
        for (case in cases) {
            assertMatchesGolden(case, golden.tolerance)
        }
    }
}
