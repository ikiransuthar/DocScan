package com.docscan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.docscan.app.data.DocFile
import com.docscan.app.ui.FileActions
import com.docscan.app.ui.Tool
import com.docscan.app.ui.components.EmptyState
import com.docscan.app.ui.components.FileRow
import com.docscan.app.ui.components.GradientButton
import com.docscan.app.ui.components.ScanningPage
import com.docscan.app.ui.components.ScreenTitle
import com.docscan.app.ui.components.ToolTile
import com.docscan.app.ui.theme.Azure
import com.docscan.app.ui.theme.Mint
import com.docscan.app.ui.theme.Mist

@Composable
fun HomeScreen(
    files: List<DocFile>,
    onScan: () -> Unit,
    onTool: (Tool) -> Unit,
    onSeeAll: () -> Unit,
    actions: FileActions
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            val count = files.size
            ScreenTitle(
                "Documents",
                if (count == 0) "Nothing here yet" else "$count file${if (count == 1) "" else "s"} on this device"
            )
        }
        item { ScanHero(onScan) }
        item { ToolsSection(onTool) }
        item { RecentSection(files.take(4), onSeeAll, actions) }
    }
}

@Composable
private fun ScanHero(onScan: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF12202B), Color(0xFF0F1A2E))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Mint.copy(alpha = 0.6f), Azure.copy(alpha = 0.15f))),
                shape
            )
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Scan a document", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Capture, crop and save as PDF in a few taps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist
                )
                Spacer(Modifier.height(18.dp))
                GradientButton("Start scan", onScan)
            }
            Spacer(Modifier.width(16.dp))
            ScanningPage(Modifier.size(width = 96.dp, height = 124.dp))
        }
    }
}

@Composable
private fun ToolsSection(onTool: (Tool) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tools", style = MaterialTheme.typography.titleMedium)
        Tool.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tool ->
                    ToolTile(tool, onClick = { onTool(tool) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentSection(recent: List<DocFile>, onSeeAll: () -> Unit, actions: FileActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (recent.isNotEmpty()) {
                Text(
                    "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onSeeAll)
                )
            }
        }
        if (recent.isEmpty()) {
            EmptyState(
                "No documents yet",
                "Tap the scan button to capture your first document. It will show up here."
            )
        } else {
            recent.forEach { FileRow(it, actions) }
        }
    }
}
