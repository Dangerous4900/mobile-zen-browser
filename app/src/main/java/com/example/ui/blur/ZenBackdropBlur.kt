package com.example.ui.blur

import android.graphics.Bitmap

/** A small, unblurred copy of the window, kept so the blur strength can change without recapturing. */
class ZenBackdropSnapshot(val bitmap: Bitmap, val sourceWidth: Int, val sourceHeight: Int)

/**
 * Builds the frosted-glass backdrop for the side panel. Everything here is safe to call off the main thread.
 *
 * The window is captured once while the panel is closed, shrunk to 1/4 size (cheap to blur, and drawn back
 * up with bilinear filtering, which looks smooth because the content is already blurred), then blurred with
 * a sigma that matches the RenderEffect blur used on the live path.
 */
object ZenBackdropBlur {

    /** Two exact 2x halvings average every source pixel properly; a single 4x bilinear step would alias. */
    fun toSnapshot(full: Bitmap): ZenBackdropSnapshot {
        val sw = full.width
        val sh = full.height
        var current = full
        repeat(2) {
            val w = (current.width / 2).coerceAtLeast(16)
            val h = (current.height / 2).coerceAtLeast(16)
            val next = Bitmap.createScaledBitmap(current, w, h, true)
            if (current !== full && current !== next) current.recycle()
            current = next
        }
        // createScaledBitmap may hand back the same instance if sizes already match; ensure we own a copy.
        val owned = if (current === full) full.copy(Bitmap.Config.ARGB_8888, false) else current
        return ZenBackdropSnapshot(owned, sw, sh)
    }

    /** @param sigmaScreenPx blur sigma in real screen pixels (same unit RenderEffect.createBlurEffect takes). */
    fun blur(snapshot: ZenBackdropSnapshot, sigmaScreenPx: Float): Bitmap {
        val src = snapshot.bitmap
        val w = src.width
        val h = src.height
        val scale = w.toFloat() / snapshot.sourceWidth.coerceAtLeast(1)
        val radius = ZenBoxBlur.radiusForSigma(sigmaScreenPx * scale)
            .coerceAtMost((minOf(w, h) / 2 - 1).coerceAtLeast(1))
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        ZenBoxBlur.blurArgb(pixels, w, h, radius)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }
}
