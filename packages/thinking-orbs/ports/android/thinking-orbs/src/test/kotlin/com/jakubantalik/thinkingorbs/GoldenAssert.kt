package com.jakubantalik.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Asserts one golden case reproduces exactly.
 *
 * Dots are compared as a canonical MULTISET plus a z-monotonicity assertion,
 * not position by position. Exact array position is a stronger claim than the
 * rendering contract makes: `breathing`@64 has an odd lane count, so its centre
 * lane sits exactly on the view plane where z is computed as
 * y*sin(tilt) + z1*cos(tilt) and the two terms cancel. That cancellation is not
 * exact in floating point, so those dots land on +/-1e-17 noise whose sign
 * depends on the platform's libm, and different platforms z-sort that tie group
 * differently. Every dot in it has the same depth, hence identical radius, ink
 * and alpha, so the order among them cannot change a pixel.
 *
 * Both sides are rounded to the golden file's 6-decimal precision BEFORE
 * sorting, so near-duplicate values sort onto the same grid on both sides —
 * otherwise the comparison misaligns and reports nonsense.
 */
fun assertMatchesGolden(case: Case, tolerance: Double) {
    val state = requireNotNull(OrbState.fromId(case.state)) { "${case.key}: unknown state" }
    val size = requireNotNull(OrbSize.fromPx(case.size)) { "${case.key}: unknown size" }

    val preset = resolvePreset(state, size)
    assertEquals("${case.key} mode", case.mode, preset.mode.id)

    val frame = orbFrame(preset, case.size.toDouble(), case.t)

    assertEquals("${case.key} dot count", case.dotCount, frame.dots.size)
    assertEquals("${case.key} line count", case.lineCount, frame.lines.size)

    val q6 = { v: Double -> Math.round(v * 1_000_000.0) / 1_000_000.0 }

    val mine = frame.dots
        .map { doubleArrayOf(it.x, it.y, it.z, it.r, it.white, it.a).map(q6) }
        .sortedWith(::lexCompare)
    val theirs = (0 until case.dotCount)
        .map { i -> (0 until 6).map { f -> q6(case.dots[i * 6 + f]) } }
        .sortedWith(::lexCompare)

    val fields = listOf("x", "y", "z", "r", "white", "a")
    for (i in mine.indices) {
        for (f in 0 until 6) {
            assertEquals(
                "${case.key} dot$i.${fields[f]}",
                theirs[i][f],
                mine[i][f],
                tolerance,
            )
        }
    }

    // The part of draw order that IS the contract: far to near.
    for (i in 1 until frame.dots.size) {
        assertTrue(
            "${case.key}: dots not z-sorted at $i",
            frame.dots[i].z >= frame.dots[i - 1].z,
        )
    }

    // Lines are few and their order IS deterministic (nested i<j loops).
    for (i in 0 until case.lineCount) {
        val b = i * 7
        val l = frame.lines[i]
        assertEquals("${case.key} line$i.x1", case.lines[b], l.x1, tolerance)
        assertEquals("${case.key} line$i.y1", case.lines[b + 1], l.y1, tolerance)
        assertEquals("${case.key} line$i.x2", case.lines[b + 2], l.x2, tolerance)
        assertEquals("${case.key} line$i.y2", case.lines[b + 3], l.y2, tolerance)
        assertEquals("${case.key} line$i.white", case.lines[b + 4], l.white, tolerance)
        assertEquals("${case.key} line$i.a", case.lines[b + 5], l.a, tolerance)
        assertEquals("${case.key} line$i.w", case.lines[b + 6], l.w, tolerance)
    }
}

private fun lexCompare(a: List<Double>, b: List<Double>): Int {
    for (i in a.indices) {
        if (a[i] != b[i]) return if (a[i] < b[i]) -1 else 1
    }
    return 0
}

/** Counts the values a case contributes, for the progress report in Task 10. */
fun Case.valueCount(): Int = dotCount * 6 + lineCount * 7
