package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudFile
import com.example.data.model.ConflictStrategy
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.FileTypeIcon
import com.example.ui.components.formatBytes
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CloudBridgeBorder
import com.example.ui.theme.CloudBridgePrimary
import com.example.ui.theme.CloudBridgeSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferWizardScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) } // 1: Browse & Select, 2: Review & Configure
    val files by viewModel.driveFiles.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDevMode by viewModel.isDevMode.collectAsState()

    var destinationFolder by remember { mutableStateOf("/CloudBridge Transfers/") }
    var conflictStrategy by remember { mutableStateOf(ConflictStrategy.SKIP_DUPLICATE) }
    var conflictDropdownExpanded by remember { mutableStateOf(false) }

    val totalSelectedBytes = selectedFiles.sumOf { it.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Step Tracker Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (step == 2) step = 1 else viewModel.navigateTo(Screen.DASHBOARD)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = if (step == 1) "Browse Google Drive" else "Review Transfer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (step == 1) "Step 1 of 2: Select files & folders" else "Step 2 of 2: Destination & Conflict Handling",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (step == 1) {
                        Button(
                            onClick = { step = 2 },
                            enabled = selectedFiles.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Review (${selectedFiles.size})")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (step == 1) {
            // STEP 1: Google Drive Browser
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar + View Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search files & folders...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.toggleGridView() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = "Toggle view",
                            tint = CloudBridgePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Breadcrumbs Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(breadcrumbs) { crumb ->
                        val isLast = breadcrumbs.lastOrNull()?.id == crumb.id
                        Text(
                            text = crumb.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLast) CloudBridgePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.navigateBreadcrumb(crumb) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        if (!isLast) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Selection Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedFiles.isEmpty()) "No items selected" else "${selectedFiles.size} selected (${formatBytes(totalSelectedBytes)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedFiles.isNotEmpty()) CloudBridgePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(onClick = { viewModel.selectAllFiles() }) {
                            Text("Select All", style = MaterialTheme.typography.labelSmall)
                        }
                        if (selectedFiles.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearFileSelection() }) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // File List or Grid
                if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "No files found in this folder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(files) { file ->
                            val isSelected = selectedFiles.any { it.id == file.id }
                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (file.isFolder) {
                                            viewModel.navigateIntoFolder(file)
                                        } else {
                                            viewModel.toggleFileSelection(file)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CloudBridgePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.toggleFileSelection(file) },
                                            modifier = Modifier.size(20.dp),
                                            colors = CheckboxDefaults.colors(checkedColor = CloudBridgePrimary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FileTypeIcon(type = file.fileType, size = 32)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (file.isFolder) "${file.childCount} items" else formatBytes(file.size),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(files) { file ->
                            val isSelected = selectedFiles.any { it.id == file.id }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (file.isFolder) {
                                            viewModel.navigateIntoFolder(file)
                                        } else {
                                            viewModel.toggleFileSelection(file)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CloudBridgePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleFileSelection(file) },
                                        colors = CheckboxDefaults.colors(checkedColor = CloudBridgePrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FileTypeIcon(type = file.fileType, size = 26)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (file.isFolder) "Folder • ${file.childCount} nested items" else "${formatBytes(file.size)} • ${formatTimestamp(file.modifiedTime)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (file.isFolder) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Open folder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // STEP 2: Transfer Review Screen (Section 13)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Transfer Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Source & Destination
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Google Drive", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = CloudBridgePrimary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(text = "Destination", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (isDevMode) "JioCloud (Simulated Dev)" else "JioCloud",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Selected Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Selected Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${selectedFiles.size} items (${selectedFiles.count { it.isFolder }} folders)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(text = "Total Payload Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = formatBytes(totalSelectedBytes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CloudBridgePrimary)
                                }
                            }
                        }
                    }
                }

                // Destination Folder Input
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Destination Folder",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = destinationFolder,
                                onValueChange = { destinationFolder = it },
                                label = { Text("JioCloud Target Folder") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nested folders from Google Drive will be preserved inside this target path.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Duplicate / Conflict Handling (Section 10)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Duplicate & Conflict Detection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Action if destination already contains a file with matching name or checksum:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            ExposedDropdownMenuBox(
                                expanded = conflictDropdownExpanded,
                                onExpandedChange = { conflictDropdownExpanded = !conflictDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = conflictStrategy.title,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conflictDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = conflictDropdownExpanded,
                                    onDismissRequest = { conflictDropdownExpanded = false }
                                ) {
                                    ConflictStrategy.values().forEach { strategy ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(strategy.title, fontWeight = FontWeight.Bold)
                                                    Text(strategy.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                conflictStrategy = strategy
                                                conflictDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Files Preview List
                item {
                    Text(
                        text = "Files to Transfer (${selectedFiles.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedFiles.take(8).forEach { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FileTypeIcon(type = file.fileType, size = 20)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (file.isFolder) "Folder" else formatBytes(file.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (selectedFiles.size > 8) {
                            Text(
                                text = "+ ${selectedFiles.size - 8} more files queued",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Back to Files")
                        }
                        Button(
                            onClick = {
                                viewModel.startTransfer(destinationFolder, conflictStrategy)
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Transfer")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
