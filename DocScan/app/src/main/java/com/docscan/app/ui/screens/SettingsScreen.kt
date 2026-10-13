package com.docscan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.docscan.app.ui.components.BeamBrush
import com.docscan.app.ui.components.ScreenTitle
import com.docscan.app.ui.theme.Hairline
import com.docscan.app.ui.theme.Ink
import com.docscan.app.ui.theme.Mint
import com.docscan.app.ui.theme.Mist

@Composable
fun SettingsScreen(
    qualityIndex: Int,
    onQuality: (Int) -> Unit,
    galleryImport: Boolean,
    onGalleryImport: (Boolean) -> Unit
) {
    val labels = listOf("Compact", "Balanced", "High")
    val hints = listOf(
        "Smallest files with lighter image quality.",
        "Good quality with a moderate file size.",
        "Sharpest images with larger files."
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ScreenTitle("Settings", "Make it work your way")

        SettingsCard("Image to PDF quality") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink)
                    .padding(4.dp)
            ) {
                labels.forEachIndexed { i, label ->
                    val selected = i == qualityIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(if (selected) Modifier.background(BeamBrush) else Modifier)
                            .clickable { onQuality(i) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Ink else Mist,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Text(hints[qualityIndex.coerceIn(0, 2)], style = MaterialTheme.typography.bodyMedium, color = Mist)
        }

        SettingsCard("Scanner") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Import from gallery", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Allow adding existing photos inside the scanner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist
                    )
                }
                Switch(
                    checked = galleryImport,
                    onCheckedChange = onGalleryImport,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink,
                        checkedTrackColor = Mint,
                        uncheckedThumbColor = Mist,
                        uncheckedTrackColor = Ink,
                        uncheckedBorderColor = Hairline
                    )
                )
            }
        }

        SettingsCard("Storage") {
            Text(
                "Your scans and converted files are kept inside the app. " +
                    "Use Save to Downloads from a file's menu to copy it to Downloads/DocScan.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist
            )
        }

        SettingsCard("About") {
            Text("DocScan 1.0", style = MaterialTheme.typography.labelLarge)
            Text(
                "Scanning and text recognition run on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Hairline.copy(alpha = 0.6f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
