package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {

    /**
     * The single most important test in this module.
     *
     * JavaScript's Math.round breaks ties toward +infinity; kotlin.math.round
     * breaks them toward even. Task 3 shows three presets where the difference
     * changes the rendered geometry.
     */
    @Test
    fun `jsRound breaks ties toward positive infinity like JavaScript`() {
        assertEquals(3.0, jsRound(2.5), 0.0)
        assertEquals(41.0, jsRound(40.5), 0.0)
        assertEquals(1.0, jsRound(0.5), 0.0)
        assertEquals(-2.0, jsRound(-2.5), 0.0)   // NOT -3.0
        assertEquals(-3.0, jsRound(-2.6), 0.0)
        assertEquals(0.0, jsRound(0.49999999999999994), 0.0)
        assertEquals(2.0, jsRound(2.4), 0.0)
    }

    @Test
    fun `jsRound preserves JavaScript's negative zero`() {
        // assertEquals(0.0, -0.0, 0.0) passes, so compare bits instead.
        fun bits(d: Double) = java.lang.Double.doubleToRawLongBits(d)
        val negZero = bits(-0.0)
        assertEquals("jsRound(-0.1)", negZero, bits(jsRound(-0.1)))
        assertEquals("jsRound(-0.4)", negZero, bits(jsRound(-0.4)))
        assertEquals("jsRound(-0.5)", negZero, bits(jsRound(-0.5)))
        assertEquals("jsRound(-0.0)", negZero, bits(jsRound(-0.0)))
        assertEquals("jsRound(0.0)", bits(0.0), bits(jsRound(0.0)))
        // and the existing magnitudes must still hold
        assertEquals(-1.0, jsRound(-0.6), 0.0)
        assertEquals(-2.0, jsRound(-2.5), 0.0)
        assertEquals(3.0, jsRound(2.5), 0.0)
        assertEquals(0.0, jsRound(0.49999999999999994), 0.0)
    }

    @Test
    fun `hashD is deterministic and in unit range`() {
        for (i in 0..50) {
            val h = hashD(i.toDouble(), 1.7)
            assertTrue("hashD($i) = $h", h >= 0.0 && h < 1.0)
            assertEquals(h, hashD(i.toDouble(), 1.7), 0.0)
        }
    }

    @Test
    fun `vnoise is smooth and bounded`() {
        for (i in 0..50) {
            val v = vnoise(i * 0.31, i * 0.17)
            assertTrue("vnoise = $v", v >= 0.0 && v <= 1.0)
        }
    }

    @Test
    fun `fibDir returns unit vectors`() {
        val out = DoubleArray(3)
        for (i in 0 until 40) {
            fibDir(i, 40, out)
            val len = Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2])
            assertEquals(1.0, len, 1e-12)
        }
    }

    @Test
    fun `angleDelta wraps to the shortest signed distance`() {
        assertEquals(0.0, angleDelta(1.0, 1.0), 1e-12)
        assertEquals(0.2, angleDelta(1.2, 1.0), 1e-12)
        // 0.1 vs 2*PI - 0.1 is a short hop across the wrap, not a long way round
        assertEquals(0.2, angleDelta(0.1, -0.1), 1e-12)
    }

    @Test
    fun `finalizeFrame culls invisible dots clamps radii and sorts far to near`() {
        val dots = mutableListOf(
            Dot(x = 0.0, y = 0.0, z = 5.0, r = 1.0, white = 0.5, a = 1.0),
            Dot(x = 0.0, y = 0.0, z = 1.0, r = 0.1, white = 0.5, a = 1.0),
            Dot(x = 0.0, y = 0.0, z = 3.0, r = 1.0, white = 0.5, a = 0.01), // culled
        )
        val lines = listOf(
            OrbLine(0.0, 0.0, 1.0, 1.0, 0.4, 0.5, 0.8),
            OrbLine(0.0, 0.0, 1.0, 1.0, 0.4, 0.01, 0.8), // culled
        )

        val frame = finalizeFrame(dots, lines, rMin = 0.3)

        assertEquals(2, frame.dots.size)
        assertEquals(1, frame.lines.size)
        assertEquals(1.0, frame.dots[0].z, 0.0)   // nearest z first (ascending)
        assertEquals(5.0, frame.dots[1].z, 0.0)
        assertEquals(0.3, frame.dots[0].r, 0.0)   // clamped up to rMin
    }

    /**
     * JS's `(a, b) => a.z - b.z` treats -0.0 and 0.0 as EQUAL, so a stable sort
     * leaves them in insertion order. Double.compareTo does not — it imposes a
     * total order where -0.0 < 0.0 — and would reorder them. Modes that place
     * dots exactly on the view plane produce both signs of zero.
     */
    @Test
    fun `finalizeFrame treats negative zero and positive zero as equal when sorting`() {
        val first = Dot(x = 1.0, y = 0.0, z = 0.0, r = 1.0, white = 0.5)
        val second = Dot(x = 2.0, y = 0.0, z = -0.0, r = 1.0, white = 0.5)
        val frame = finalizeFrame(mutableListOf(first, second), emptyList())

        assertEquals(1.0, frame.dots[0].x, 0.0)
        assertEquals(2.0, frame.dots[1].x, 0.0)
    }
}
