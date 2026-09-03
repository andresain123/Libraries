package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Test

class LatticeGoldenTest {

    @Test
    fun `globe matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("globe")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }

    @Test
    fun `rubik matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("rubik")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }

    @Test
    fun `wave matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val cases = golden.casesFor("wave")
        assertEquals(8, cases.size)
        for (case in cases) assertMatchesGolden(case, golden.tolerance)
    }
}
