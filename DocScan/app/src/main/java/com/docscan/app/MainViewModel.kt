package com.docscan.app

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.app.data.DocFile
import com.docscan.app.data.DocStore
import com.docscan.app.data.PdfEngine
import com.docscan.app.data.formatSize
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context = app.applicationContext
    private val store = DocStore(ctx)
    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var files by mutableStateOf<List<DocFile>>(emptyList())
        private set
    var tab by mutableIntStateOf(0)
    var busy by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
    var ocrText by mutableStateOf<String?>(null)

    var qualityIndex by mutableIntStateOf(prefs.getInt("quality", 1))
        private set
    var galleryImport by mutableStateOf(prefs.getBoolean("gallery", true))
        private set

    private val jpegQuality: Float
        get() = floatArrayOf(0.55f, 0.75f, 0.9f)[qualityIndex.coerceIn(0, 2)]

    init {
        refresh()
    }

    fun refresh() {
        files = store.list()
    }

    fun setQuality(index: Int) {
        qualityIndex = index
        prefs.edit().putInt("quality", index).apply()
    }

    fun setGalleryImport(value: Boolean) {
        galleryImport = value
        prefs.edit().putBoolean("gallery", value).apply()
    }

    val scannerOptions: GmsDocumentScannerOptions
        get() = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(galleryImport)
            .setPageLimit(30)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

    // ---------- background task helper ----------

    private fun runTask(label: String, block: suspend () -> String) {
        viewModelScope.launch {
            busy = label
            try {
                message = withContext(Dispatchers.IO) { block() }
            } catch (e: Throwable) {
                message = "Failed: ${e.message ?: "something went wrong"}"
            } finally {
                busy = null
                refresh()
            }
        }
    }

    private fun displayName(uri: Uri): String {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getString(0)
                if (!n.isNullOrBlank()) return store.cleanName(n.substringBeforeLast('.'))
            }
        }
        return store.stamp("Document")
    }

    // ---------- features ----------

    fun onScanResult(result: GmsDocumentScanningResult?) {
        val pdfUri = result?.pdf?.uri ?: return
        runTask("Saving scan…") {
            val out = store.newFile(store.stamp("Scan"), "pdf")
            val input = ctx.contentResolver.openInputStream(pdfUri) ?: error("Could not read the scan")
            input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            "Saved ${out.name}"
        }
    }

    fun imagesToPdf(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val quality = jpegQuality
        runTask("Creating PDF…") {
            val out = store.newFile(store.stamp("Images"), "pdf")
            PdfEngine.imagesToPdf(ctx, uris, out, quality) { i, n -> busy = "Adding page $i of $n…" }
            "Created ${out.name}"
        }
    }

    fun pdfToImages(uri: Uri) {
        runTask("Converting pages…") {
            val list = PdfEngine.pdfToImages(ctx, uri, store, displayName(uri)) { i, n ->
                busy = "Page $i of $n…"
            }
            "Saved ${list.size} image${if (list.size == 1) "" else "s"}"
        }
    }

    fun merge(uris: List<Uri>) {
        if (uris.size < 2) {
            message = "Pick at least 2 PDFs to merge"
            return
        }
        runTask("Merging PDFs…") {
            val out = store.newFile(store.stamp("Merged"), "pdf")
            PdfEngine.mergePdfs(ctx, uris, out)
            "Merged ${uris.size} PDFs"
        }
    }

    fun split(uri: Uri) {
        runTask("Splitting PDF…") {
            val n = PdfEngine.splitPdf(ctx, uri, store, displayName(uri))
            "Created $n file${if (n == 1) "" else "s"}"
        }
    }

    fun compress(uri: Uri) {
        runTask("Compressing…") {
            val before = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            val out = store.newFile("${displayName(uri)}_compressed", "pdf")
            PdfEngine.compressPdf(ctx, uri, out, 0.6f) { i, n -> busy = "Page $i of $n…" }
            if (before > 0) "${formatSize(before)} → ${formatSize(out.length())}"
            else "Saved ${out.name} (${formatSize(out.length())})"
        }
    }

    fun extractText(uri: Uri) {
        busy = "Reading text…"
        try {
            val image = InputImage.fromFilePath(ctx, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { result ->
                    busy = null
                    if (result.text.isBlank()) message = "No text found in this image"
                    else ocrText = result.text
                }
                .addOnFailureListener {
                    busy = null
                    message = "Could not read text from this image"
                }
        } catch (e: Exception) {
            busy = null
            message = "Could not open the image"
        }
    }

    // ---------- file actions ----------

    fun delete(doc: DocFile) {
        doc.file.delete()
        refresh()
        message = "Deleted ${doc.name}"
    }

    fun rename(doc: DocFile, newName: String) {
        val clean = store.cleanName(newName)
        val target = File(doc.file.parentFile, "$clean.${doc.file.extension}")
        if (target != doc.file && target.exists()) {
            message = "A file with that name already exists"
            return
        }
        message = if (doc.file.renameTo(target)) "Renamed" else "Could not rename"
        refresh()
    }

    fun saveToDevice(doc: DocFile) {
        runTask("Saving to Downloads…") {
            val resolver = ctx.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, doc.name)
                put(MediaStore.Downloads.MIME_TYPE, doc.mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DocScan")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create the file")
            val output = resolver.openOutputStream(uri) ?: error("Could not write the file")
            output.use { o -> doc.file.inputStream().use { it.copyTo(o) } }
            "Saved to Downloads/DocScan"
        }
    }
}
