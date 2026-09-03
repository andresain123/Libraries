package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Test

class StrandsGoldenTest {

    @Test
    fun `braid matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("braid")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }

    @Test
    fun `ribbon matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("ribbon")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }

    /**
     * `ring` shares ribbon's geometry via the faceOn flag, and `breathing`@64 is
     * the case whose centre lane sits exactly on the view plane — the tie group
     * that made position-wise comparison the wrong contract.
     */
    @Test
    fun `ring matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("ring")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }

    @Test
    fun `ring carries no ghost sphere`() {
        assertEquals(
            0.0,
            resolvePreset(OrbState.Breathing, OrbSize.Px64).opts.getValue("ghostN"),
            0.0,
        )
    }
}
