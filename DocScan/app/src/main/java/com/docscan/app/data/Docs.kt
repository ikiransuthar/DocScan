package com.docscan.app.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DocFile(
    val file: File,
    val modified: Long = file.lastModified(),
    val size: Long = file.length()
) {
    val name: String get() = file.name
    val isPdf: Boolean get() = file.extension.equals("pdf", ignoreCase = true)
    val meta: String get() = "${formatSize(size)} · ${formatDate(modified)}"
    val mime: String
        get() = when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "txt" -> "text/plain"
            else -> "*/*"
        }
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

fun formatDate(ms: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = ms }
    val now = Calendar.getInstance()
    if (sameDay(now, then)) return "Today"
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(yesterday, then)) return "Yesterday"
    return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
}

/** All documents live in the app's private storage: filesDir/docs */
class DocStore(context: Context) {
    val dir: File = File(context.filesDir, "docs").apply { mkdirs() }

    fun list(): List<DocFile> {
        val allowed = setOf("pdf", "jpg", "jpeg", "png", "txt")
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in allowed }
            ?.sortedByDescending { it.lastModified() }
            ?.map { DocFile(it) }
            ?: emptyList()
    }

    fun cleanName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Document" }

    fun newFile(baseName: String, ext: String): File {
        val safe = cleanName(baseName)
        var f = File(dir, "$safe.$ext")
        var i = 1
        while (f.exists()) {
            f = File(dir, "$safe ($i).$ext")
            i++
        }
        return f
    }

    fun stamp(prefix: String): String =
        prefix + "_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
