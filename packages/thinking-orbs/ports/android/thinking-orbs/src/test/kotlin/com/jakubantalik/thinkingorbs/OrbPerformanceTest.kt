package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A coarse floor, not a benchmark. The JVM here is a desktop one, so this only
 * catches an accidental order-of-magnitude regression (an O(n^2) slipped into a
 * mode, a per-dot allocation in a hot loop). Real device numbers come from
 * profiling on a representative mid-tier Android device.
 */
class OrbPerformanceTest {

    @Test
    fun `the heaviest mode builds a frame well inside a frame budget`() {
        // composing@64 is the densest mode in the set
        val preset = resolvePreset(OrbState.Composing, OrbSize.Px64)

        repeat(200) { orbFrame(preset, 64.0, it * 0.017) }  // warm up the JIT

        val started = System.nanoTime()
        val iterations = 1_000
        repeat(iterations) { orbFrame(preset, 64.0, it * 0.017) }
        val perFrameMicros = (System.nanoTime() - started) / iterations / 1_000.0

        println("composing@64: %.1f us/frame".format(perFrameMicros))
        assertTrue(
            "geometry took %.1f us/frame, expected well under a 16.6 ms budget"
                .format(perFrameMicros),
            perFrameMicros < 2_000.0,
        )
    }

    @Test
    fun `every state builds without throwing at several instants`() {
        for (state in OrbState.entries) {
            for (size in OrbSize.entries) {
                for (t in listOf(0.0, 0.6, 1.7, 3.3, 5.1, 60.0, 3_600.0)) {
                    val frame = orbFrame(state, size, t)
                    assertTrue("$state/$size/@$t produced no dots", frame.dots.isNotEmpty())
                }
            }
        }
    }
}
