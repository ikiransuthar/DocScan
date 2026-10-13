package com.docscan.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docscan.app.data.DocFile
import com.docscan.app.data.Thumbs
import com.docscan.app.ui.FileActions
import com.docscan.app.ui.Tool
import com.docscan.app.ui.theme.Azure
import com.docscan.app.ui.theme.Cloud
import com.docscan.app.ui.theme.Coral
import com.docscan.app.ui.theme.Hairline
import com.docscan.app.ui.theme.Ink
import com.docscan.app.ui.theme.InkCard
import com.docscan.app.ui.theme.Mint
import com.docscan.app.ui.theme.Mist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val BeamBrush: Brush = Brush.linearGradient(listOf(Mint, Azure))

@Composable
fun ScreenTitle(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BeamBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ink, style = MaterialTheme.typography.labelLarge)
    }
}

/** The one memorable element: a page being scanned by a moving beam. */
@Composable
fun ScanningPage(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "beam"
    )
    Canvas(modifier.clip(RoundedCornerShape(14.dp))) {
        drawRoundRect(Color(0xFFEAF0F8), cornerRadius = CornerRadius(14.dp.toPx()))

        val pad = 14.dp.toPx()
        val lineH = 5.dp.toPx()
        var y = pad + 6.dp.toPx()
        listOf(0.55f, 1f, 1f, 0.8f, 1f, 0.65f).forEach { w ->
            drawRoundRect(
                Color(0xFFC3CEDD),
                topLeft = Offset(pad, y),
                size = Size((size.width - pad * 2) * w, lineH),
                cornerRadius = CornerRadius(lineH / 2)
            )
            y += lineH + 9.dp.toPx()
        }

        val beamY = size.height * progress
        val glow = 28.dp.toPx()
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, Mint.copy(alpha = 0.35f)),
                startY = beamY - glow,
                endY = beamY
            ),
            topLeft = Offset(0f, beamY - glow),
            size = Size(size.width, glow)
        )
        drawRect(
            Mint,
            topLeft = Offset(0f, beamY - 1.5.dp.toPx()),
            size = Size(size.width, 3.dp.toPx())
        )
    }
}

@Composable
fun ToolTile(tool: Tool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Hairline.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(tool.icon, contentDescription = null, tint = Mint, modifier = Modifier.size(22.dp))
        }
        Text(
            tool.title,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FileRow(doc: DocFile, actions: FileActions, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    val accent = if (doc.isPdf) Coral else Azure
    val thumb by produceState<ImageBitmap?>(null, doc.file.path, doc.modified) {
        value = withContext(Dispatchers.IO) { Thumbs.load(doc.file)?.asImageBitmap() }
    }

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { actions.open(doc) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            val t = thumb
            if (t != null) {
                Image(
                    bitmap = t,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    if (doc.isPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.Photo,
                    contentDescription = null,
                    tint = accent
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                doc.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(doc.meta, style = MaterialTheme.typography.labelMedium, color = Mist)
        }
        Icon(
            Icons.Rounded.Share,
            contentDescription = "Share ${doc.name}",
            tint = Mist,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { actions.share(doc) }
                .padding(9.dp)
        )
        Box {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "More options",
                tint = Mist,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { menu = true }
                    .padding(8.dp)
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = { menu = false; actions.share(doc) }
                )
                DropdownMenuItem(
                    text = { Text("Save to Downloads") },
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    onClick = { menu = false; actions.save(doc) }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = { menu = false; actions.rename(doc) }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Coral) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Coral) },
                    onClick = { menu = false; actions.delete(doc) }
                )
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = Mint, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Mist, textAlign = TextAlign.Center)
    }
}

@Composable
fun BusyOverlay(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC0A0E14))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .clip(shape)
                .background(InkCard)
                .border(1.dp, Hairline, shape)
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Mint, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun MessagePill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(50))
            .background(Cloud)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(text, color = Ink, style = MaterialTheme.typography.labelLarge, maxLines = 2)
    }
}

private data class NavEntry(val icon: ImageVector, val label: String)

private val navEntries = listOf(
    NavEntry(Icons.Rounded.Home, "Home"),
    NavEntry(Icons.Rounded.Folder, "Files"),
    NavEntry(Icons.Rounded.GridView, "Tools"),
    NavEntry(Icons.Rounded.Settings, "Settings")
)

@Composable
fun FloatingNav(
    selected: Int,
    onSelect: (Int) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(34.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(barShape)
                .background(Color(0xF2171E2A))
                .border(1.dp, Hairline, barShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(navEntries[0], selected == 0, Modifier.weight(1f)) { onSelect(0) }
            NavItem(navEntries[1], selected == 1, Modifier.weight(1f)) { onSelect(1) }
            Spacer(Modifier.weight(1f))
            NavItem(navEntries[2], selected == 2, Modifier.weight(1f)) { onSelect(2) }
            NavItem(navEntries[3], selected == 3, Modifier.weight(1f)) { onSelect(3) }
        }
        Box(
            Modifier
                .offset(y = (-16).dp)
                .size(66.dp)
                .shadow(20.dp, CircleShape, ambientColor = Mint, spotColor = Mint)
                .clip(CircleShape)
                .background(BeamBrush)
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.DocumentScanner,
                contentDescription = "Scan",
                tint = Ink,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    entry: NavEntry,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color by animateColorAsState(if (selected) Mint else Mist, label = "navColor")
    Column(
        modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(entry.icon, contentDescription = entry.label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(entry.label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
