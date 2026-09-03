package com.jakubantalik.thinkingorbs

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * Loader for spec/orbs-golden.json — the reference engine's exact output at
 * four fixed timestamps for every (state × size) pair.
 *
 * Dot stride 6: x, y, z, r, white, a.
 * Line stride 7: x1, y1, x2, y2, white, a, w.
 * Dots are in draw order (z ascending); lines draw first.
 */
class Golden(
    val specVersion: String,
    val tolerance: Double,
    val cases: List<Case>,
    val resolved: Map<String, ResolvedCase>,
) {
    fun casesFor(vararg modes: String): List<Case> =
        cases.filter { it.mode in modes }
}

class Case(
    val key: String,
    val state: String,
    val size: Int,
    val mode: String,
    val t: Double,
    @SerializedName("dotCount") val dotCount: Int,
    @SerializedName("lineCount") val lineCount: Int,
    val dots: DoubleArray,
    val lines: DoubleArray,
)

class ResolvedCase(
    val mode: String,
    val speed: Double,
    val opts: Map<String, Double>,
)

object GoldenFixture {
    fun load(): Golden {
        val stream = requireNotNull(
            GoldenFixture::class.java.classLoader?.getResourceAsStream("orbs-golden.json")
        ) { "orbs-golden.json missing from test resources" }
        return InputStreamReader(stream).use { Gson().fromJson(it, Golden::class.java) }
    }
}
