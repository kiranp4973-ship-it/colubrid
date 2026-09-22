package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransferJobEntity
import com.example.data.model.ConnectionStatus
import com.example.data.model.JobStatus
import com.example.data.model.ProviderType
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.ProviderStatusBadge
import com.example.ui.components.RealtimeProtocolBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CloudBridgeError
import com.example.ui.theme.CloudBridgePrimary
import com.example.ui.theme.CloudBridgeSecondary
import com.example.ui.theme.CloudBridgeSuccess
import com.example.ui.theme.CloudBridgeWarning

@Composable
fun DashboardScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val connections by viewModel.providerConnections.collectAsState()
    val jobs by viewModel.transferJobs.collectAsState()
    val isDevMode by viewModel.isDevMode.collectAsState()
    val currentSpeed by viewModel.currentTransferSpeed.collectAsState()

    val realtimeProgress by viewModel.realtimeProgress.collectAsState()
    val realtimeProtocol by viewModel.realtimeProtocol.collectAsState()
    val realtimeConnectionStatus by viewModel.realtimeConnectionStatus.collectAsState()
    val realtimePollingIntervalMs by viewModel.realtimePollingIntervalMs.collectAsState()

    // Active or most recent transfer for live telemetry summary
    val activeJob = jobs.find {
        it.status in listOf(JobStatus.TRANSFERRING, JobStatus.PREPARING, JobStatus.QUEUED, JobStatus.VERIFYING, JobStatus.PAUSED)
    } ?: jobs.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Welcome Back Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back, ${currentUser?.displayName ?: "Guest"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Secure cloud-to-cloud synchronization dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isDevMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CloudBridgePrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "DEV MODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CloudBridgePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Quick Transfer Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quick Transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Browse your Google Drive and transfer files directly to JioCloud destination.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.TRANSFER_WIZARD) },
                        colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start New Transfer")
                    }
                }
            }
        }

        // Connected Clouds Section
        item {
            SectionHeader(
                title = "Connected Clouds",
                subtitle = "Manage cloud account connections & authorization",
                action = {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CloudBridgePrimary,
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.CONNECTIONS) }
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            val gDriveConn = connections.find { it.provider == ProviderType.GOOGLE_DRIVE }
            val jioConn = connections.find { it.provider == ProviderType.JIO_CLOUD }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Google Drive Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(Screen.CONNECTIONS) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                            }
                            ProviderStatusBadge(status = gDriveConn?.status ?: ConnectionStatus.CONNECTED)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Google Drive", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = gDriveConn?.accountEmail ?: "user.cloudbridge@gmail.com",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Used: 28.4 GB / 100 GB",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // JioCloud Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(Screen.CONNECTIONS) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            }
                            if (isDevMode) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                    Text(
                                        text = "Dev Mock",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = CloudBridgeSuccess
                                    )
                                }
                            } else {
                                ProviderStatusBadge(status = jioConn?.status ?: ConnectionStatus.NOT_CONFIGURED)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "JioCloud", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isDevMode) "Simulated Dev Target" else "Not configured yet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDevMode) "Ready for test transfers" else "Requires official API",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isDevMode) CloudBridgeSuccess else CloudBridgeWarning
                        )
                    }
                }
            }
        }

        // Transfer Summary Section (Section 11)
        item {
            SectionHeader(
                title = "Transfer Summary",
                subtitle = if (activeJob != null) "Current Pipeline: ${activeJob.sourceProvider.displayName} → ${activeJob.destinationProvider.displayName}" else "No active transfer running"
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (activeJob != null) {
                ActiveTransferTelemetryCard(
                    job = activeJob,
                    liveSpeed = currentSpeed,
                    realtimeProgress = realtimeProgress,
                    realtimeProtocol = realtimeProtocol,
                    realtimeConnectionStatus = realtimeConnectionStatus,
                    pollingIntervalMs = realtimePollingIntervalMs,
                    onViewDetail = { viewModel.viewJobDetail(activeJob.id) }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = CloudBridgeSecondary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No transfers in progress",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Select files from Google Drive to launch a direct cloud transfer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(onClick = { viewModel.triggerDemoTransfer() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch 14.8 GB Demo Pipeline")
                        }
                    }
                }
            }
        }

        // Recent Transfers Section
        item {
            SectionHeader(
                title = "Recent Transfers",
                subtitle = "Recent sync history and verification records",
                action = {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CloudBridgePrimary,
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.HISTORY) }
                    )
                }
            )
        }

        if (jobs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Your transfers will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(jobs.take(4)) { job ->
                RecentTransferCard(job = job, onClick = { viewModel.viewJobDetail(job.id) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ActiveTransferTelemetryCard(
    job: TransferJobEntity,
    liveSpeed: Long,
    realtimeProgress: com.example.data.realtime.RealtimeTransferProgress?,
    realtimeProtocol: com.example.data.realtime.RealtimeProtocol,
    realtimeConnectionStatus: com.example.data.realtime.RealtimeConnectionStatus,
    pollingIntervalMs: Long,
    onViewDetail: () -> Unit
) {
    val isLive = realtimeProgress != null && realtimeProgress.jobId == job.id && (job.status == JobStatus.TRANSFERRING || job.status == JobStatus.PREPARING)

    val totalFiles = if (isLive) realtimeProgress!!.totalFiles else job.totalFiles
    val completedFiles = if (isLive) realtimeProgress!!.filesCompleted else job.completedFiles
    val failedFiles = if (isLive) realtimeProgress!!.failedFiles else job.failedFiles
    val skippedFiles = if (isLive) realtimeProgress!!.skippedFiles else job.skippedFiles
    val remainingFiles = if (isLive) realtimeProgress!!.filesRemaining else maxOf(0, totalFiles - completedFiles - failedFiles - skippedFiles)

    val transferredBytes = if (isLive) realtimeProgress!!.bytesTransferred else job.transferredBytes
    val totalBytes = if (isLive) realtimeProgress!!.totalBytes else job.totalBytes

    val progressFraction = if (totalBytes > 0) {
        (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displaySpeed = if (isLive && realtimeProgress!!.transferSpeedBytesPerSec > 0) {
        realtimeProgress.transferSpeedBytesPerSec
    } else if (liveSpeed > 0) liveSpeed else job.currentSpeedBytesPerSec

    val remainingBytes = maxOf(0L, totalBytes - transferredBytes)
    val etaSec = if (isLive && realtimeProgress!!.etaSeconds > 0) {
        realtimeProgress.etaSeconds
    } else if (displaySpeed > 0) remainingBytes / displaySpeed else 0L

    val etaText = if (etaSec > 0 && job.status == JobStatus.TRANSFERRING) {
        "${etaSec / 60}m ${etaSec % 60}s"
    } else if (job.status == JobStatus.COMPLETED) "Done" else if (job.status == JobStatus.PAUSED) "Paused" else "Calculating..."

    val currentFileName = if (isLive) realtimeProgress!!.currentFileName else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Status, Route, Realtime Protocol Badge, Percent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    StatusBadge(status = job.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${job.sourceProvider.displayName} → ${job.destinationProvider.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (job.status == JobStatus.TRANSFERRING || job.status == JobStatus.PREPARING) {
                        RealtimeProtocolBadge(
                            protocol = realtimeProtocol,
                            connectionStatus = realtimeConnectionStatus,
                            pollingIntervalMs = pollingIntervalMs
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CloudBridgePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CloudBridgePrimary,
                trackColor = CloudBridgePrimary.copy(alpha = 0.15f)
            )

            // Current File Being Streamed (if active)
            if (!currentFileName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CloudBridgePrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentFileName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isLive && realtimeProgress!!.currentFileTotalBytes > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatBytes(realtimeProgress.currentFileBytes)} / ${formatBytes(realtimeProgress.currentFileTotalBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6 Grid Metrics as specified in Section 11: Files, Completed, Remaining, Failed, Data, Speed, ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Total Files", "$totalFiles")
                MetricColumn("Completed", "$completedFiles", CloudBridgeSuccess)
                MetricColumn("Remaining", "$remainingFiles")
                MetricColumn("Failed", "$failedFiles", if (failedFiles > 0) CloudBridgeError else MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Transferred", "${formatBytes(transferredBytes)} / ${formatBytes(totalBytes)}")
                MetricColumn("Speed", formatSpeed(displaySpeed), CloudBridgePrimary)
                MetricColumn("ETA", etaText)
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onViewDetail,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Real-Time Pipeline & Controls")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun RecentTransferCard(
    job: TransferJobEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CloudBridgePrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = CloudBridgePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${job.sourceProvider.displayName} → ${job.destinationProvider.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${job.totalFiles} files • ${formatBytes(job.totalBytes)} • ${formatTimestamp(job.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatusBadge(status = job.status)
        }
    }
}
