package com.docscan.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import java.io.File

object Thumbs {
    private val cache = LruCache<String, Bitmap>(48)

    fun load(file: File, sizePx: Int = 180): Bitmap? {
        val key = "${file.path}|${file.lastModified()}"
        cache.get(key)?.let { return it }
        val bmp = try {
            if (file.extension.equals("pdf", ignoreCase = true)) pdfThumb(file, sizePx) else imageThumb(file, sizePx)
        } catch (e: Throwable) {
            null
        }
        if (bmp != null) cache.put(key, bmp)
        return bmp
    }

    private fun pdfThumb(file: File, size: Int): Bitmap? =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) {
                    null
                } else {
                    renderer.openPage(0).use { page ->
                        val scale = size.toFloat() / page.width
                        val bmp = Bitmap.createBitmap(
                            size,
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }

    private fun imageThumb(file: File, size: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= size) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.path, opts)
    }
}
