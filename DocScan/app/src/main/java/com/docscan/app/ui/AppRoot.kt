package com.docscan.app.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.docscan.app.MainViewModel
import com.docscan.app.data.DocFile
import com.docscan.app.data.Sharing
import com.docscan.app.ui.components.BusyOverlay
import com.docscan.app.ui.components.FloatingNav
import com.docscan.app.ui.components.MessagePill
import com.docscan.app.ui.screens.FilesScreen
import com.docscan.app.ui.screens.HomeScreen
import com.docscan.app.ui.screens.SettingsScreen
import com.docscan.app.ui.screens.ToolsScreen
import com.docscan.app.ui.theme.Coral
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.delay

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var pdfTool by rememberSaveable { mutableStateOf<Tool?>(null) }
    var renaming by remember { mutableStateOf<DocFile?>(null) }
    var deleting by remember { mutableStateOf<DocFile?>(null) }

    // ---------- launchers ----------

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.onScanResult(GmsDocumentScanningResult.fromActivityResultIntent(result.data))
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> -> vm.imagesToPdf(uris) }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            when (pdfTool) {
                Tool.PdfToImage -> vm.pdfToImages(uri)
                Tool.Split -> vm.split(uri)
                Tool.Compress -> vm.compress(uri)
                else -> Unit
            }
        }
    }

    val multiPdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> -> vm.merge(uris) }

    val ocrPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) vm.extractText(uri) }

    fun startScan() {
        val activity = context as? Activity
        if (activity == null) {
            vm.message = "Scanner can't start right now"
            return
        }
        GmsDocumentScanning.getClient(vm.scannerOptions)
            .getStartScanIntent(activity)
            .addOnSuccessListener { sender ->
                scanLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener {
                vm.message = "Scanner isn't available on this device"
            }
    }

    fun onTool(tool: Tool) {
        val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        when (tool) {
            Tool.ImageToPdf -> imagePicker.launch(imageOnly)
            Tool.Merge -> multiPdfPicker.launch(arrayOf("application/pdf"))
            Tool.ExtractText -> ocrPicker.launch(imageOnly)
            else -> {
                pdfTool = tool
                pdfPicker.launch(arrayOf("application/pdf"))
            }
        }
    }

    val actions = remember(vm, context) {
        FileActions(
            open = { Sharing.open(context, it) },
            share = { Sharing.share(context, listOf(it)) },
            save = { vm.saveToDevice(it) },
            rename = { renaming = it },
            delete = { deleting = it }
        )
    }

    BackHandler(enabled = vm.tab != 0) { vm.tab = 0 }

    // ---------- layout ----------

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Crossfade(targetState = vm.tab, label = "tab") { tab ->
            when (tab) {
                0 -> HomeScreen(
                    files = vm.files,
                    onScan = { startScan() },
                    onTool = { onTool(it) },
                    onSeeAll = { vm.tab = 1 },
                    actions = actions
                )
                1 -> FilesScreen(files = vm.files, actions = actions)
                2 -> ToolsScreen(onTool = { onTool(it) })
                else -> SettingsScreen(
                    qualityIndex = vm.qualityIndex,
                    onQuality = { vm.setQuality(it) },
                    galleryImport = vm.galleryImport,
                    onGalleryImport = { vm.updateGalleryImport(it) }
                )
            }
        }

        FloatingNav(
            selected = vm.tab,
            onSelect = { vm.tab = it },
            onScan = { startScan() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        val msg = vm.message
        if (msg != null) {
            MessagePill(
                msg,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 112.dp)
            )
            LaunchedEffect(msg) {
                delay(2600)
                vm.message = null
            }
        }

        val busy = vm.busy
        if (busy != null) BusyOverlay(busy)
    }

    // ---------- dialogs ----------

    renaming?.let { doc ->
        var name by remember(doc) { mutableStateOf(doc.file.nameWithoutExtension) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.rename(doc, name)
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Cancel") }
            }
        )
    }

    deleting?.let { doc ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete file?") },
            text = { Text("${doc.name} will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(doc)
                    deleting = null
                }) { Text("Delete", color = Coral) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }

    vm.ocrText?.let { text ->
        AlertDialog(
            onDismissRequest = { vm.ocrText = null },
            title = { Text("Extracted text") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(text))
                    vm.message = "Copied to clipboard"
                    vm.ocrText = null
                }) { Text("Copy") }
            },
            dismissButton = {
                androidx.compose.foundation.layout.Row {
                    TextButton(onClick = { Sharing.shareText(context, text) }) { Text("Share") }
                    TextButton(onClick = { vm.ocrText = null }) { Text("Close") }
                }
            }
        )
    }
}
