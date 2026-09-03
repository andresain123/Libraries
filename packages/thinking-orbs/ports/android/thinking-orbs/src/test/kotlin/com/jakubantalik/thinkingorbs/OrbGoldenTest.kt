package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Asserts the hand-transcribed Kotlin engine reproduces the reference engine's
 * geometry exactly, dot by dot, against spec/orbs-golden.json.
 *
 * This is the safety net that makes transcribing ~1,200 lines of maths by hand
 * a reasonable thing to do at all: a mistyped constant or a sign error fails
 * here with the exact case and field, instead of shipping as an animation that
 * merely looks a bit off.
 */
class OrbGoldenTest {

    @Test
    fun `every state and size matches the golden vectors`() {
        val golden = GoldenFixture.load()
        val failures = mutableListOf<String>()
        var checked = 0

        for (case in golden.cases) {
            checked += case.valueCount()
            try {
                assertMatchesGolden(case, golden.tolerance)
            } catch (e: AssertionError) {
                failures += "${case.key}: ${e.message}"
            }
        }

        assertEquals("all 72 cases were visited", 72, golden.cases.size)
        assertEquals("all 70,115 values were compared", 70_115, checked)

        if (failures.isNotEmpty()) {
            fail(
                "${failures.size} case(s) failed of ${golden.cases.size}:\n" +
                    failures.take(25).joinToString("\n")
            )
        }
        println(
            "golden: ${golden.cases.size} cases, $checked values within " +
                "${golden.tolerance} (spec ${golden.specVersion})"
        )
    }

    @Test
    fun `every state resolves to a distinct mode mapping`() {
        val golden = GoldenFixture.load()
        for (case in golden.cases) {
            val state = requireNotNull(OrbState.fromId(case.state))
            assertEquals(case.key, case.mode, state.mode.id)
        }
    }
}
