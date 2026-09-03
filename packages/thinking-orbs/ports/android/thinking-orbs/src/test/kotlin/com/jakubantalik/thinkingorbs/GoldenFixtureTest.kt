package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldenFixtureTest {

    @Test
    fun `loads the golden fixture`() {
        val golden = GoldenFixture.load()

        assertEquals("1.0.0", golden.specVersion)
        assertEquals(1e-4, golden.tolerance, 0.0)
        assertEquals(72, golden.cases.size)
        assertEquals(18, golden.resolved.size)
    }

    @Test
    fun `fixture carries 70115 comparable values`() {
        val golden = GoldenFixture.load()
        val total = golden.cases.sumOf { it.dotCount * 6 + it.lineCount * 7 }
        assertEquals(70_115, total)
    }

    @Test
    fun `only the web mode emits lines`() {
        val golden = GoldenFixture.load()
        val withLines = golden.cases.filter { it.lineCount > 0 }.map { it.mode }.toSet()
        assertEquals(setOf("web"), withLines)
    }

    @Test
    fun `every case declares array lengths matching its counts`() {
        val golden = GoldenFixture.load()
        for (c in golden.cases) {
            assertEquals("${c.key} dots", c.dotCount * 6, c.dots.size)
            assertEquals("${c.key} lines", c.lineCount * 7, c.lines.size)
        }
        assertTrue(golden.cases.isNotEmpty())
    }
}
