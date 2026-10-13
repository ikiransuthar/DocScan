package com.docscan.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.ui.graphics.vector.ImageVector
import com.docscan.app.data.DocFile

enum class Tool(val title: String, val description: String, val icon: ImageVector) {
    ImageToPdf("Image to PDF", "Combine photos into one PDF", Icons.Rounded.PictureAsPdf),
    PdfToImage("PDF to image", "Save every page as a JPG", Icons.Rounded.Photo),
    Merge("Merge PDFs", "Join several PDFs into one", Icons.Rounded.MergeType),
    Split("Split PDF", "Turn each page into its own PDF", Icons.Rounded.CallSplit),
    Compress("Compress", "Make a large PDF smaller", Icons.Rounded.Compress),
    ExtractText("Extract text", "Copy the text from a photo", Icons.Rounded.TextFields)
}

class FileActions(
    val open: (DocFile) -> Unit,
    val share: (DocFile) -> Unit,
    val save: (DocFile) -> Unit,
    val rename: (DocFile) -> Unit,
    val delete: (DocFile) -> Unit
)
