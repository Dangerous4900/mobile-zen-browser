package com.example.ui.blur

/**
 * Pure-Kotlin approximate Gaussian blur (three box-blur passes per axis) for ARGB_8888 pixel arrays.
 * Divisions round to nearest (truncating would darken every image by ~1% over six passes).
 * Edges are clamped, matching RenderEffect's TileMode.CLAMP. No Android dependencies so it is unit-testable.
 *
 * Three box passes of radius r give a variance of r * (r + 1), i.e. sigma ~= r + 0.5.
 */
object ZenBoxBlur {

    fun radiusForSigma(sigma: Float): Int = Math.round(sigma - 0.5f)

    /** Blurs [pixels] (row-major, width*height ARGB) in place. A radius < 1 is a no-op. */
    fun blurArgb(pixels: IntArray, width: Int, height: Int, radius: Int, passes: Int = 3) {
        if (radius < 1 || width < 1 || height < 1 || pixels.size < width * height) return
        val tmp = IntArray(width * height)
        repeat(passes) {
            boxHorizontal(pixels, tmp, width, height, radius)
            boxVertical(tmp, pixels, width, height, radius)
        }
    }

    private fun boxHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        val half = div / 2
        for (y in 0 until h) {
            val row = y * w
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) {
                val p = src[row + clamp(i, w)]
                sa += p ushr 24; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
            }
            for (x in 0 until w) {
                dst[row + x] = pack((sa + half) / div, (sr + half) / div, (sg + half) / div, (sb + half) / div)
                val add = src[row + clamp(x + r + 1, w)]
                val rem = src[row + clamp(x - r, w)]
                sa += (add ushr 24) - (rem ushr 24)
                sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                sb += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }

    private fun boxVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        val half = div / 2
        for (x in 0 until w) {
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) {
                val p = src[clamp(i, h) * w + x]
                sa += p ushr 24; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
            }
            for (y in 0 until h) {
                dst[y * w + x] = pack((sa + half) / div, (sr + half) / div, (sg + half) / div, (sb + half) / div)
                val add = src[clamp(y + r + 1, h) * w + x]
                val rem = src[clamp(y - r, h) * w + x]
                sa += (add ushr 24) - (rem ushr 24)
                sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                sb += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }

    private fun clamp(i: Int, n: Int): Int = if (i < 0) 0 else if (i >= n) n - 1 else i

    private fun pack(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b
}
