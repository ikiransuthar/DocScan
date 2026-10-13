package com.docscan.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.docscan.app.data.DocFile
import com.docscan.app.ui.FileActions
import com.docscan.app.ui.components.EmptyState
import com.docscan.app.ui.components.FileRow
import com.docscan.app.ui.components.ScreenTitle
import com.docscan.app.ui.theme.Mint

@Composable
fun FilesScreen(files: List<DocFile>, actions: FileActions) {
    var query by remember { mutableStateOf("") }
    val shown = remember(files, query) {
        if (query.isBlank()) files else files.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            ScreenTitle("Files", "${files.size} saved on this device")
            Spacer(Modifier.height(16.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search files") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = com.docscan.app.ui.theme.InkCard,
                    unfocusedContainerColor = com.docscan.app.ui.theme.InkCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Mint
                )
            )
        }
        if (shown.isEmpty()) {
            EmptyState(
                if (files.isEmpty()) "No files yet" else "No matches",
                if (files.isEmpty()) "Scanned and converted files will be listed here."
                else "Try a different name."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown, key = { it.file.path }) { doc -> FileRow(doc, actions) }
            }
        }
    }
}
