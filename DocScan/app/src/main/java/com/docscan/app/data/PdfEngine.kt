package com.docscan.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.io.File
import java.io.FileOutputStream

object PdfEngine {

    /** Photos -> one PDF. Images are stored as JPEG inside the PDF, so files stay small. */
    fun imagesToPdf(
        context: Context,
        uris: List<Uri>,
        out: File,
        quality: Float,
        onProgress: (Int, Int) -> Unit
    ) {
        val doc = PDDocument()
        try {
            uris.forEachIndexed { index, uri ->
                onProgress(index + 1, uris.size)
                val bmp = decodeScaled(context, uri, 2200)
                try {
                    val w = 595f
                    val h = w * bmp.height / bmp.width
                    val page = PDPage(PDRectangle(w, h))
                    doc.addPage(page)
                    val img = JPEGFactory.createFromImage(doc, bmp, quality)
                    PDPageContentStream(doc, page).use { it.drawImage(img, 0f, 0f, w, h) }
                } finally {
                    bmp.recycle()
                }
            }
            doc.save(out)
        } finally {
            doc.close()
        }
    }

    /** PDF -> one JPG per page. */
    fun pdfToImages(
        context: Context,
        uri: Uri,
        store: DocStore,
        baseName: String,
        onProgress: (Int, Int) -> Unit
    ): List<File> {
        val result = mutableListOf<File>()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open the PDF")
        pfd.use {
            PdfRenderer(it).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    onProgress(i + 1, renderer.pageCount)
                    renderer.openPage(i).use { page ->
                        val s = minOf(2.5f, 3000f / maxOf(page.width, page.height))
                        val bmp = Bitmap.createBitmap(
                            (page.width * s).toInt().coerceAtLeast(1),
                            (page.height * s).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val f = store.newFile("${baseName}_p${i + 1}", "jpg")
                        FileOutputStream(f).use { os -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, os) }
                        bmp.recycle()
                        result.add(f)
                    }
                }
            }
        }
        return result
    }

    /** Re-renders every page as a lower-resolution JPEG. Text becomes an image (not selectable). */
    fun compressPdf(
        context: Context,
        uri: Uri,
        out: File,
        quality: Float,
        onProgress: (Int, Int) -> Unit
    ) {
        val doc = PDDocument()
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open the PDF")
            pfd.use {
                PdfRenderer(it).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        onProgress(i + 1, renderer.pageCount)
                        renderer.openPage(i).use { page ->
                            val pw = page.width.toFloat()
                            val ph = page.height.toFloat()
                            val s = minOf(1.4f, 2000f / maxOf(page.width, page.height))
                            val bmp = Bitmap.createBitmap(
                                (page.width * s).toInt().coerceAtLeast(1),
                                (page.height * s).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val pdPage = PDPage(PDRectangle(pw, ph))
                            doc.addPage(pdPage)
                            val img = JPEGFactory.createFromImage(doc, bmp, quality)
                            PDPageContentStream(doc, pdPage).use { cs -> cs.drawImage(img, 0f, 0f, pw, ph) }
                            bmp.recycle()
                        }
                    }
                }
            }
            doc.save(out)
        } finally {
            doc.close()
        }
    }

    fun mergePdfs(context: Context, uris: List<Uri>, out: File) {
        val temps = mutableListOf<File>()
        try {
            val merger = PDFMergerUtility()
            uris.forEachIndexed { i, uri ->
                val tmp = File(context.cacheDir, "merge_$i.pdf")
                copyUriToFile(context, uri, tmp)
                temps.add(tmp)
                merger.addSource(tmp)
            }
            merger.destinationFileName = out.absolutePath
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
        } finally {
            temps.forEach { it.delete() }
        }
    }

    /** Every page becomes its own PDF. Returns how many files were created. */
    fun splitPdf(context: Context, uri: Uri, store: DocStore, baseName: String): Int {
        val tmp = File(context.cacheDir, "split_src.pdf")
        copyUriToFile(context, uri, tmp)
        val doc = PDDocument.load(tmp)
        var count = 0
        try {
            val parts = Splitter().split(doc)
            parts.forEachIndexed { i, part ->
                part.save(store.newFile("${baseName}_page${i + 1}", "pdf"))
                part.close()
                count++
            }
        } finally {
            doc.close()
            tmp.delete()
        }
        return count
    }

    private fun copyUriToFile(context: Context, uri: Uri, dest: File) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Could not read the file")
        input.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
    }

    private fun decodeScaled(context: Context, uri: Uri, maxSide: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }
}
