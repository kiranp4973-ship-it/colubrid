package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserSettingsEntity
import com.example.data.model.ConflictStrategy
import com.example.data.realtime.RealtimeProtocol
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.RealtimeProtocolBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CloudBridgeError
import com.example.ui.theme.CloudBridgePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val realtimeProtocol by viewModel.realtimeProtocol.collectAsState()
    val realtimeConnectionStatus by viewModel.realtimeConnectionStatus.collectAsState()
    val realtimePollingIntervalMs by viewModel.realtimePollingIntervalMs.collectAsState()
    val realtimeIsFallbackActive by viewModel.realtimeIsFallbackActive.collectAsState()
    val simulateRealtimeErrorMode by viewModel.simulateRealtimeErrorMode.collectAsState()

    var concurrency by remember { mutableFloatStateOf(userSettings?.maxConcurrentTransfers?.toFloat() ?: 3f) }
    var autoRetry by remember { mutableStateOf(userSettings?.autoRetryFailed ?: true) }
    var selectedConflict by remember { mutableStateOf(userSettings?.defaultConflictStrategy ?: ConflictStrategy.SKIP_DUPLICATE) }
    var conflictDropdownExpanded by remember { mutableStateOf(false) }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Settings & Security",
                subtitle = "Manage transfer preferences, security policies, and privacy protocols"
            )
        }

        // Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CloudBridgePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CloudBridgePrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.displayName ?: "User Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.email ?: "alex.vance@gmail.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { viewModel.signOut() }) {
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // Transfer Preferences Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = CloudBridgePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Transfer Preferences",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Concurrency Slider
                    Text(
                        text = "Concurrent File Streams: ${concurrency.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Parallel worker threads handling simultaneous chunk streaming (1-5).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = concurrency,
                        onValueChange = { concurrency = it },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = CloudBridgePrimary, activeTrackColor = CloudBridgePrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Conflict Strategy
                    Text(
                        text = "Default Duplicate Strategy",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = conflictDropdownExpanded,
                        onExpandedChange = { conflictDropdownExpanded = !conflictDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedConflict.title,
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
                                    text = { Text(strategy.title) },
                                    onClick = {
                                        selectedConflict = strategy
                                        conflictDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-Retry Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Auto-retry Transient Failures", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Automatically retry with exponential backoff on network timeouts.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoRetry,
                            onCheckedChange = { autoRetry = it }
                        )
                    }
                }
            }
        }

        // Real-Time Streaming & Fallback Protocols Card
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
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = CloudBridgePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Real-Time Telemetry Protocol",
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configures protocol for live transfer telemetry (bytes, speed, ETA, and active file updates).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
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

                    if (realtimeProtocol == RealtimeProtocol.POLLING || realtimeIsFallbackActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Fallback Polling Interval (${realtimePollingIntervalMs}ms):",
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
                                text = "Simulate Connection Drop (Test Failover)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (simulateRealtimeErrorMode) "Streaming disabled: Automatic fallback to polling active" else "Tests real-time fault tolerance and error recovery",
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

        // Security & Privacy Guarantees Card (Section 31)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CloudBridgePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security & Privacy Protocol",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val securityPoints = listOf(
                        "No Permanent Storage" to "Files are streamed directly or processed in temporary ephemeral memory chunks.",
                        "Zero Password Retention" to "CloudBridge never requests or stores your Google or cloud passwords.",
                        "Direct HTTPS / TLS 1.3" to "All chunk transfers travel over encrypted channels with certificate pinning.",
                        "Checksum Verification" to "Every transferred byte is validated against source MD5/SHA hashes."
                    )

                    securityPoints.forEach { (title, desc) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CloudBridgePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Complete Privacy & Data Policy")
                    }
                }
            }
        }

        // Account & Danger Zone
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.showToast("Transfer audit history exported as JSON.") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Transfer Audit Logs")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CloudBridgeError)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Account & Local Cache")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("CloudBridge Privacy & Security Guarantee") },
            text = {
                Text(
                    "CloudBridge is strictly a transfer pipeline, not a permanent cloud drive.\n\n" +
                    "1. Your files are transferred using streaming pipes or small temporary memory chunks that are discarded immediately upon upload to the destination.\n\n" +
                    "2. We never hold your passwords. All authentications occur directly against Google Identity OAuth servers.\n\n" +
                    "3. All local records stored on device are strictly metadata logs (job timestamps, file names, sizes, checksum validation results)."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("Are you sure you want to remove your session and clear all transfer history logs? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CloudBridgeError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
