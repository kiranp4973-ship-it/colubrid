package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransferJobEntity
import com.example.data.model.JobStatus
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatBytes
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CloudBridgeError
import com.example.ui.theme.CloudBridgePrimary

@Composable
fun HistoryScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val jobs by viewModel.transferJobs.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Transfer History",
                subtitle = "Complete audit record of cloud transfer jobs and reports"
            )
        }

        if (jobs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transfers recorded yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed and in-progress transfers will be catalogued here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.TRANSFER_WIZARD) },
                            colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary)
                        ) {
                            Text("Start a Transfer")
                        }
                    }
                }
            }
        } else {
            items(jobs) { job ->
                HistoryJobCard(
                    job = job,
                    onViewDetails = { viewModel.viewJobDetail(job.id) },
                    onRetryFailed = { viewModel.retryFailedItems(job.id) },
                    onDelete = { viewModel.deleteTransferJob(job.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HistoryJobCard(
    job: TransferJobEntity,
    onViewDetails: () -> Unit,
    onRetryFailed: () -> Unit,
    onDelete: () -> Unit
) {
    val durationMs = if (job.completedAt != null && job.startedAt != null) {
        maxOf(1000L, job.completedAt - job.startedAt)
    } else 0L

    val durationText = if (durationMs > 0) {
        val sec = durationMs / 1000
        if (sec < 60) "${sec}s" else "${sec / 60}m ${sec % 60}s"
    } else "In progress"

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
                Column {
                    Text(
                        text = "${job.sourceProvider.displayName} → ${job.destinationProvider.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTimestamp(job.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Files", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${job.completedFiles} / ${job.totalFiles}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Total Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatBytes(job.totalBytes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(durationText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Failed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${job.failedFiles}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (job.failedFiles > 0) CloudBridgeError else MaterialTheme.colorScheme.onSurface)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (job.failedFiles > 0) {
                        OutlinedButton(
                            onClick = onRetryFailed,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry Failed", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary)
                    ) {
                        Text("View Details", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
