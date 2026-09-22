package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.TransferEventEntity
import com.example.data.local.TransferItemEntity
import com.example.data.local.TransferJobEntity
import com.example.data.model.ItemStatus
import com.example.data.model.JobStatus
import com.example.data.model.VerificationStatus
import com.example.data.realtime.RealtimeConnectionLog
import com.example.data.realtime.RealtimeProtocol
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.ItemStatusBadge
import com.example.ui.components.RealtimeProtocolBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CloudBridgeError
import com.example.ui.theme.CloudBridgePrimary
import com.example.ui.theme.CloudBridgeSuccess
import com.example.ui.theme.CloudBridgeWarning

@Composable
fun TransferDetailScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val job by viewModel.activeJob.collectAsState()
    val items by viewModel.activeJobItems.collectAsState()
    val events by viewModel.activeJobEvents.collectAsState()
    val liveSpeed by viewModel.currentTransferSpeed.collectAsState()

    val realtimeProgress by viewModel.realtimeProgress.collectAsState()
    val realtimeProtocol by viewModel.realtimeProtocol.collectAsState()
    val realtimeConnectionStatus by viewModel.realtimeConnectionStatus.collectAsState()
    val realtimeConnectionLogs by viewModel.realtimeConnectionLogs.collectAsState()
    val realtimePollingIntervalMs by viewModel.realtimePollingIntervalMs.collectAsState()
    val realtimeIsFallbackActive by viewModel.realtimeIsFallbackActive.collectAsState()
    val simulateRealtimeErrorMode by viewModel.simulateRealtimeErrorMode.collectAsState()

    var detailTab by remember { mutableIntStateOf(0) } // 0: Files & Items, 1: Live Event Logs, 2: Real-time Telemetry Logs

    if (job == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No transfer job selected", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { viewModel.navigateTo(Screen.DASHBOARD) }) {
                    Text("Return to Dashboard")
                }
            }
        }
        return
    }

    val currentJob = job!!
    val isLive = (currentJob.status == JobStatus.TRANSFERRING || currentJob.status == JobStatus.PREPARING) &&
        realtimeProgress != null && realtimeProgress!!.jobId == currentJob.id

    val totalFiles = if (isLive) realtimeProgress!!.totalFiles else currentJob.totalFiles
    val completedFiles = if (isLive) realtimeProgress!!.filesCompleted else currentJob.completedFiles
    val failedFiles = if (isLive) realtimeProgress!!.failedFiles else currentJob.failedFiles
    val skippedFiles = if (isLive) realtimeProgress!!.skippedFiles else currentJob.skippedFiles
    val remainingFiles = if (isLive) realtimeProgress!!.filesRemaining else maxOf(0, totalFiles - completedFiles - failedFiles - skippedFiles)

    val transferredBytes = if (isLive) realtimeProgress!!.bytesTransferred else currentJob.transferredBytes
    val totalBytes = if (isLive) realtimeProgress!!.totalBytes else currentJob.totalBytes

    val progressFraction = if (totalBytes > 0) {
        (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displaySpeed = if (isLive && realtimeProgress!!.transferSpeedBytesPerSec > 0) {
        realtimeProgress!!.transferSpeedBytesPerSec
    } else if (liveSpeed > 0) liveSpeed else currentJob.currentSpeedBytesPerSec

    val remainingBytes = maxOf(0L, totalBytes - transferredBytes)
    val etaSec = if (isLive && realtimeProgress!!.etaSeconds > 0) {
        realtimeProgress!!.etaSeconds
    } else if (displaySpeed > 0) remainingBytes / displaySpeed else 0L

    val etaText = if (etaSec > 0 && currentJob.status == JobStatus.TRANSFERRING) {
        "${etaSec / 60}m ${etaSec % 60}s"
    } else if (currentJob.status == JobStatus.COMPLETED) "Completed" else if (currentJob.status == JobStatus.PAUSED) "Paused" else "—"

    val currentFileName = if (isLive) realtimeProgress!!.currentFileName else null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Transfer Pipeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentJob.sourceProvider.displayName} → ${currentJob.destinationProvider.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(status = currentJob.status)
            }
        }

        // Live Overall Progress Card (Section 14 & Real-Time Specifications)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Overall Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentJob.status == JobStatus.TRANSFERRING || currentJob.status == JobStatus.PREPARING) {
                                Spacer(modifier = Modifier.height(4.dp))
                                RealtimeProtocolBadge(
                                    protocol = realtimeProtocol,
                                    connectionStatus = realtimeConnectionStatus,
                                    pollingIntervalMs = realtimePollingIntervalMs
                                )
                            }
                        }
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CloudBridgePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CloudBridgePrimary,
                        trackColor = CloudBridgePrimary.copy(alpha = 0.15f)
                    )

                    // Current Streaming File (Section: Current file name & streaming chunk progress)
                    if (!currentFileName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = CloudBridgePrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Active File Transfer",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = currentFileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (isLive && realtimeProgress!!.currentFileTotalBytes > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${formatBytes(realtimeProgress!!.currentFileBytes)} / ${formatBytes(realtimeProgress!!.currentFileTotalBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid Metrics: Completed, Remaining, Failed, Data, Speed, ETA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$completedFiles / $totalFiles", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CloudBridgeSuccess)
                        }
                        Column {
                            Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$remainingFiles", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Failed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$failedFiles", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (failedFiles > 0) CloudBridgeError else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Transferred", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${formatBytes(transferredBytes)} / ${formatBytes(totalBytes)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatSpeed(displaySpeed), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CloudBridgePrimary)
                        }
                        Column {
                            Text("ETA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(etaText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Destination Path
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Destination: ${currentJob.destinationPath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Transfer Controls (Section 14)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (currentJob.status) {
                            JobStatus.TRANSFERRING, JobStatus.PREPARING, JobStatus.QUEUED -> {
                                OutlinedButton(
                                    onClick = { viewModel.pauseTransfer(currentJob.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.cancelTransfer(currentJob.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CloudBridgeError)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel")
                                }
                            }
                            JobStatus.PAUSED -> {
                                Button(
                                    onClick = { viewModel.resumeTransfer(currentJob.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.cancelTransfer(currentJob.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CloudBridgeError)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel")
                                }
                            }
                            JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED -> {
                                if (failedFiles > 0) {
                                    Button(
                                        onClick = { viewModel.retryFailedItems(currentJob.id) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry Failed ($failedFiles)")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Post Transfer Summary Banner if Completed
        if (currentJob.status == JobStatus.COMPLETED) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CloudBridgeSuccess, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transfer Completed Successfully",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CloudBridgeSuccess
                            )
                            Text(
                                text = "All transferred files passed byte size and checksum verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Real-Time Transport & Protocol Telemetry Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = CloudBridgePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Real-Time Telemetry Transport",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        RealtimeProtocolBadge(
                            protocol = realtimeProtocol,
                            connectionStatus = realtimeConnectionStatus,
                            pollingIntervalMs = realtimePollingIntervalMs
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Select active streaming protocol between backend worker and frontend client:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = realtimeProtocol == RealtimeProtocol.WEBSOCKET,
                            onClick = { viewModel.setRealtimeProtocol(RealtimeProtocol.WEBSOCKET) },
                            label = { Text("⚡ WebSocket") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = realtimeProtocol == RealtimeProtocol.SSE,
                            onClick = { viewModel.setRealtimeProtocol(RealtimeProtocol.SSE) },
                            label = { Text("🌊 SSE Stream") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = realtimeProtocol == RealtimeProtocol.POLLING,
                            onClick = { viewModel.setRealtimeProtocol(RealtimeProtocol.POLLING) },
                            label = { Text("🔄 Polling") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Polling interval selector (if polling is chosen or fallback active)
                    if (realtimeProtocol == RealtimeProtocol.POLLING || realtimeIsFallbackActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Polling Refresh Interval (${realtimePollingIntervalMs}ms):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(500L, 1000L, 2000L, 5000L).forEach { interval ->
                                FilterChip(
                                    selected = realtimePollingIntervalMs == interval,
                                    onClick = { viewModel.setPollingInterval(interval) },
                                    label = { Text("${interval}ms") }
                                )
                            }
                        }
                    }

                    // Diagnostic simulation for failover/recovery testing
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulate Socket Drop (Test Polling Fallback)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (simulateRealtimeErrorMode) "Simulating drop: Client automatically failed over to Polling fallback" else "Verifies resilient fallback if streaming encounters an error",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (simulateRealtimeErrorMode) CloudBridgeError else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = simulateRealtimeErrorMode,
                            onCheckedChange = { viewModel.toggleSimulateRealtimeError(it) }
                        )
                    }
                }
            }
        }

        // Tabs: Per-File Item List vs Live Events vs Real-Time Connection Logs
        item {
            TabRow(
                selectedTabIndex = detailTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CloudBridgePrimary
            ) {
                Tab(
                    selected = detailTab == 0,
                    onClick = { detailTab = 0 },
                    text = { Text("Files (${items.size})") }
                )
                Tab(
                    selected = detailTab == 1,
                    onClick = { detailTab = 1 },
                    text = { Text("Audit Events (${events.size})") }
                )
                Tab(
                    selected = detailTab == 2,
                    onClick = { detailTab = 2 },
                    text = { Text("Live Stream Logs (${realtimeConnectionLogs.size})") }
                )
            }
        }

        if (detailTab == 0) {
            // Per-file transfer list (Section 14 & 16)
            if (items.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No items found for this transfer.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(items) { item ->
                    FileTransferItemCard(item = item)
                }
            }
        } else if (detailTab == 1) {
            // Live Audit Event Logs
            if (events.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No events recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(events) { event ->
                    EventLogCard(event = event)
                }
            }
        } else {
            // Real-Time Connection & Protocol Logs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time Telemetry Stream Logs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { viewModel.clearRealtimeLogs() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs")
                    }
                }
            }

            if (realtimeConnectionLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No realtime stream logs recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(realtimeConnectionLogs) { log ->
                    RealtimeLogCard(log = log)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FileTransferItemCard(item: TransferItemEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (item.isFolder) Color(0xFFF59E0B) else CloudBridgePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                ItemStatusBadge(status = item.status, verification = item.verificationStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!item.isFolder && item.status == ItemStatus.TRANSFERRING) {
                LinearProgressIndicator(
                    progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CloudBridgePrimary,
                    trackColor = CloudBridgePrimary.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.isFolder) "Folder" else "${formatBytes(item.bytesTransferred)} / ${formatBytes(item.fileSize)} • ${item.percentage}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.verificationStatus == VerificationStatus.VERIFIED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = CloudBridgeSuccess, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Checksum Verified",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = CloudBridgeSuccess
                        )
                    }
                }
            }

            if (item.error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Error: ${item.error}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CloudBridgeError
                )
            }
        }
    }
}

@Composable
fun EventLogCard(event: TransferEventEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = formatTimestamp(event.timestamp).substringAfter("• "),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = event.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RealtimeLogCard(log: RealtimeConnectionLog) {
    val tagColor = when (log.level) {
        com.example.data.model.LogLevel.ERROR -> CloudBridgeError
        com.example.data.model.LogLevel.WARN -> Color(0xFFF59E0B)
        com.example.data.model.LogLevel.INFO -> if (log.message.contains("Connect") || log.message.contains("established")) Color(0xFF10B981) else CloudBridgePrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tagColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.protocol.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = tagColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.level.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = tagColor
                    )
                }
                Text(
                    text = formatTimestamp(log.timestamp).substringAfter("• "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (log.level == com.example.data.model.LogLevel.ERROR) CloudBridgeError else MaterialTheme.colorScheme.onSurface
            )

            if (!log.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

