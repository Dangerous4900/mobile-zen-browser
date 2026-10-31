package com.example.ui.blur

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ZenBoxBlurTest {

    @Test
    fun constantImageIsUnchanged() {
        for (r in listOf(1, 3, 12, 40)) {
            val px = IntArray(37 * 29) { 0xFF336699.toInt() }
            ZenBoxBlur.blurArgb(px, 37, 29, r)
            assertTrue("r=$r", px.all { it == 0xFF336699.toInt() })
        }
    }

    @Test
    fun blurMatchesExactThreePassBoxConvolutionAndKeepsBrightness() {
        val w = 201
        val h = 201
        val r = 8
        val px = IntArray(w * h) { 0xFF000000.toInt() }
        for (y in 80..120) for (x in 80..120) px[y * w + x] = 0xFFFFFFFF.toInt()
        ZenBoxBlur.blurArgb(px, w, h, r)

        fun conv(a: DoubleArray) = DoubleArray(a.size) { i ->
            var acc = 0.0
            for (k in -r..r) acc += a[(i + k).coerceIn(0, a.size - 1)]
            acc / (2 * r + 1)
        }
        var prof = DoubleArray(w) { if (it in 80..120) 1.0 else 0.0 }
        repeat(3) { prof = conv(prof) }
        for (y in listOf(100, 90, 75, 125)) for (x in 60..140) {
            val expected = 255 * prof[x] * prof[y]
            assertEquals("($x,$y)", expected, (px[y * w + x] and 0xFF).toDouble(), 3.0)
        }
        // rounding (not truncating) keeps overall brightness
        val sum = px.sumOf { (it and 0xFF).toLong() }
        val expectedSum = 41L * 41 * 255
        assertTrue(abs(sum - expectedSum) < expectedSum * 0.005)
    }

    @Test
    fun radiusLargerThanImageStaysUniformAndDoesNotCrash() {
        val px = IntArray(5 * 4) { if (it % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
        ZenBoxBlur.blurArgb(px, 5, 4, 50)
        val first = px[0] and 0xFF
        assertTrue(px.all { abs((it and 0xFF) - first) <= 3 })
    }

    @Test
    fun radiusZeroIsNoOp() {
        val px = IntArray(9) { it }
        ZenBoxBlur.blurArgb(px, 3, 3, 0)
        assertEquals((0 until 9).toList(), px.toList())
    }
}
