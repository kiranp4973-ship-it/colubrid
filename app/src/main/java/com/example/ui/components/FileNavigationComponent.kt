package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProviderConnectionEntity
import com.example.data.model.CloudFile
import com.example.data.model.CloudFileType
import com.example.data.model.ConnectionStatus
import com.example.data.model.ProviderType
import com.example.ui.Breadcrumb
import com.example.ui.CloudBridgeViewModel
import com.example.ui.theme.CloudBridgeBorder
import com.example.ui.theme.CloudBridgePrimary
import com.example.ui.theme.CloudBridgePrimaryContainer
import com.example.ui.theme.CloudBridgeSuccess
import com.example.ui.theme.CloudBridgeSuccessContainer
import com.example.ui.theme.CloudBridgeWarning
import com.example.ui.theme.CloudBridgeWarningContainer

/**
 * Production-ready file navigation component in Jetpack Compose.
 * Allows users to browse directories, list files, filter by category/search,
 * and select files and folders from connected Google Drive and JioCloud accounts.
 */
@Composable
fun FileNavigationComponent(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null
) {
    val selectedProvider by viewModel.selectedBrowserProvider.collectAsState()
    val files by viewModel.browserFiles.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val isLoading by viewModel.isLoadingDrive.collectAsState()
    val browserError by viewModel.browserError.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val connections by viewModel.providerConnections.collectAsState()

    val currentConnection = connections.find { it.provider == selectedProvider }

    FileNavigationContent(
        selectedProvider = selectedProvider,
        onSelectProvider = { viewModel.selectBrowserProvider(it) },
        files = files,
        breadcrumbs = breadcrumbs,
        selectedFiles = selectedFiles,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        isGridView = isGridView,
        onToggleGridView = { viewModel.toggleGridView() },
        isLoading = isLoading,
        errorMessage = browserError,
        selectedCategory = selectedCategory,
        onCategorySelect = { viewModel.toggleCategoryFilter(it) },
        onFolderClick = { viewModel.navigateIntoFolder(it) },
        onBreadcrumbClick = { viewModel.navigateBreadcrumb(it) },
        onNavigateUp = { viewModel.navigateUp() },
        onRefresh = { viewModel.refreshBrowser() },
        onFileToggleSelect = { viewModel.toggleFileSelection(it) },
        onSelectAll = { viewModel.selectAllFiles() },
        onClearSelection = { viewModel.clearFileSelection() },
        connectionEntity = currentConnection,
        primaryActionLabel = primaryActionLabel,
        onPrimaryAction = onPrimaryAction,
        modifier = modifier
    )
}

@Composable
fun FileNavigationContent(
    selectedProvider: ProviderType,
    onSelectProvider: (ProviderType) -> Unit,
    files: List<CloudFile>,
    breadcrumbs: List<Breadcrumb>,
    selectedFiles: Set<CloudFile>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    selectedCategory: CloudFileType?,
    onCategorySelect: (CloudFileType?) -> Unit,
    onFolderClick: (CloudFile) -> Unit,
    onBreadcrumbClick: (Breadcrumb) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onFileToggleSelect: (CloudFile) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    connectionEntity: ProviderConnectionEntity?,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null
) {
    // Filter files by category if active
    val displayedFiles = if (selectedCategory != null) {
        files.filter { it.fileType == selectedCategory }
    } else {
        files
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Provider Account Selector Tabs & Quota Bar
        AccountSelectorHeader(
            selectedProvider = selectedProvider,
            onSelectProvider = onSelectProvider,
            connectionEntity = connectionEntity
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Search Field & View Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search files & folders...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("file_search_clear_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("file_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CloudBridgePrimary,
                    unfocusedBorderColor = CloudBridgeBorder
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Grid / List Toggle Button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CloudBridgeBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleGridView)
                    .testTag("file_view_toggle"),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                        contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                        tint = CloudBridgePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Refresh Button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CloudBridgeBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onRefresh)
                    .testTag("file_browser_refresh"),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All (${files.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CloudBridgePrimaryContainer,
                        selectedLabelColor = CloudBridgePrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_all")
                )
            }
            item {
                FilterChip(
                    selected = selectedCategory == CloudFileType.FOLDER,
                    onClick = { onCategorySelect(CloudFileType.FOLDER) },
                    label = { Text("Folders (${files.count { it.isFolder }})") },
                    leadingIcon = {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CloudBridgePrimaryContainer,
                        selectedLabelColor = CloudBridgePrimary
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedCategory == CloudFileType.DOCUMENT,
                    onClick = { onCategorySelect(CloudFileType.DOCUMENT) },
                    label = { Text("Docs (${files.count { it.fileType == CloudFileType.DOCUMENT }})") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategory == CloudFileType.PDF,
                    onClick = { onCategorySelect(CloudFileType.PDF) },
                    label = { Text("PDFs (${files.count { it.fileType == CloudFileType.PDF }})") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategory == CloudFileType.IMAGE,
                    onClick = { onCategorySelect(CloudFileType.IMAGE) },
                    label = { Text("Images (${files.count { it.fileType == CloudFileType.IMAGE }})") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategory == CloudFileType.VIDEO,
                    onClick = { onCategorySelect(CloudFileType.VIDEO) },
                    label = { Text("Videos (${files.count { it.fileType == CloudFileType.VIDEO }})") }
                )
            }
            item {
                FilterChip(
                    selected = selectedCategory == CloudFileType.ARCHIVE,
                    onClick = { onCategorySelect(CloudFileType.ARCHIVE) },
                    label = { Text("Archives (${files.count { it.fileType == CloudFileType.ARCHIVE }})") }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Breadcrumb Navigation Bar & Batch Selection Controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home button
                IconButton(
                    onClick = {
                        breadcrumbs.firstOrNull()?.let { onBreadcrumbClick(it) }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("breadcrumb_home")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Root Folder",
                        tint = CloudBridgePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Up navigation button
                if (breadcrumbs.size > 1) {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("breadcrumb_up")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Up one folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Breadcrumbs trail
                LazyRow(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(breadcrumbs) { crumb ->
                        val isLast = breadcrumbs.lastOrNull()?.id == crumb.id
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = crumb.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLast) CloudBridgePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onBreadcrumbClick(crumb) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            if (!isLast) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Select All / Clear Quick Buttons
                Text(
                    text = if (selectedFiles.isEmpty()) "Select All" else "Clear",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CloudBridgePrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (selectedFiles.isEmpty()) onSelectAll() else onClearSelection()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag(if (selectedFiles.isEmpty()) "file_select_all" else "file_clear_selection")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Main Content Area (Loading, Error, Empty, List or Grid)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = CloudBridgePrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Loading files from ${selectedProvider.displayName}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = CloudBridgeWarningContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = CloudBridgeWarning,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connection Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Connection")
                        }
                    }
                }

                displayedFiles.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No files match \"$searchQuery\"" else "Folder is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { onSearchQueryChange("") }) {
                                Text("Clear search filter")
                            }
                        }
                    }
                }

                isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedFiles, key = { it.id }) { file ->
                            val isSelected = selectedFiles.any { it.id == file.id }
                            FileGridCard(
                                file = file,
                                isSelected = isSelected,
                                onToggleSelect = { onFileToggleSelect(file) },
                                onOpen = { if (file.isFolder) onFolderClick(file) else onFileToggleSelect(file) }
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedFiles, key = { it.id }) { file ->
                            val isSelected = selectedFiles.any { it.id == file.id }
                            FileListItemRow(
                                file = file,
                                isSelected = isSelected,
                                onToggleSelect = { onFileToggleSelect(file) },
                                onOpen = { if (file.isFolder) onFolderClick(file) else onFileToggleSelect(file) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Selection Summary & Primary Action Bar
        AnimatedVisibility(
            visible = selectedFiles.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedFiles.size} item${if (selectedFiles.size == 1) "" else "s"} selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val totalBytes = selectedFiles.sumOf { it.size }
                        Text(
                            text = if (totalBytes > 0) formatBytes(totalBytes) else "Folders only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onClearSelection,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.bodySmall)
                        }

                        if (primaryActionLabel != null && onPrimaryAction != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onPrimaryAction,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(primaryActionLabel, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Account Selector Header with connected badge and storage quota indicator.
 */
@Composable
fun AccountSelectorHeader(
    selectedProvider: ProviderType,
    onSelectProvider: (ProviderType) -> Unit,
    connectionEntity: ProviderConnectionEntity?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Segmented account tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Google Drive Tab
                val isDrive = selectedProvider == ProviderType.GOOGLE_DRIVE
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectProvider(ProviderType.GOOGLE_DRIVE) }
                        .testTag("account_tab_google_drive"),
                    color = if (isDrive) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isDrive) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Drive",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isDrive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDrive) CloudBridgePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // JioCloud Tab
                val isJio = selectedProvider == ProviderType.JIO_CLOUD
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectProvider(ProviderType.JIO_CLOUD) }
                        .testTag("account_tab_jio_cloud"),
                    color = if (isJio) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isJio) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JioCloud",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isJio) FontWeight.Bold else FontWeight.Normal,
                            color = if (isJio) CloudBridgePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Account status and storage quota
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = connectionEntity?.accountName ?: "${selectedProvider.displayName} Account",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectionEntity?.accountEmail ?: if (selectedProvider == ProviderType.GOOGLE_DRIVE) "user.cloudbridge@gmail.com" else "developer.simulated@jiocloud.mock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val status = connectionEntity?.status ?: ConnectionStatus.CONNECTED
                ProviderStatusBadge(status = status)
            }

            // Storage quota progress bar
            val usedBytes = connectionEntity?.storageUsedBytes ?: if (selectedProvider == ProviderType.GOOGLE_DRIVE) 28_400_000_000L else 12_800_000_000L
            val totalBytes = connectionEntity?.storageTotalBytes ?: 100_000_000_000L
            val progress = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CloudBridgePrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatBytes(usedBytes)} used of ${formatBytes(totalBytes)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * List Item representation for a file or folder.
 */
@Composable
fun FileListItemRow(
    file: CloudFile,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CloudBridgePrimary else CloudBridgeBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                if (isSelected) CloudBridgePrimaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onOpen)
            .testTag("file_item_${file.id}"),
        color = if (isSelected) CloudBridgePrimaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox with minimum 48dp touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onToggleSelect)
                    .testTag("file_item_checkbox_${file.id}"),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = CloudBridgePrimary)
                )
            }

            // File Icon
            FileTypeIcon(type = file.fileType, size = 26)

            Spacer(modifier = Modifier.width(12.dp))

            // File Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (file.isFolder) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (file.isFolder) "${file.childCount} items" else formatBytes(file.size),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(file.modifiedTime),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Folder chevron drill-down indicator
            if (file.isFolder) {
                IconButton(onClick = onOpen) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Grid Item representation for a file or folder.
 */
@Composable
fun FileGridCard(
    file: CloudFile,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CloudBridgePrimary else CloudBridgeBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onOpen)
            .testTag("file_item_${file.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CloudBridgePrimaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Checkbox and type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = file.fileType.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onToggleSelect)
                        .testTag("file_item_checkbox_${file.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = CloudBridgePrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Center large icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        FileTypeIcon(type = file.fileType, size = 30)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File Name
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (file.isFolder) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Size / Child Count
            Text(
                text = if (file.isFolder) "${file.childCount} items" else formatBytes(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
