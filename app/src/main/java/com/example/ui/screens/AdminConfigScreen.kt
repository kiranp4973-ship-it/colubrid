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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AuditLogEntity
import com.example.data.model.UserRole
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.formatBytes
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CloudBridgeError
import com.example.ui.theme.CloudBridgePrimary
import com.example.ui.theme.CloudBridgeSuccess
import com.example.ui.theme.CloudBridgeWarning

@Composable
fun AdminConfigScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDevMode by viewModel.isDevMode.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val totalJobs by viewModel.totalJobsCount.collectAsState()
    val completedJobs by viewModel.completedJobsCount.collectAsState()
    val failedJobs by viewModel.failedJobsCount.collectAsState()
    val activeJobs by viewModel.activeJobsCount.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var apiBaseUrl by remember(userSettings) { mutableStateOf(userSettings?.jioCloudApiBaseUrl ?: "") }
    var clientId by remember(userSettings) { mutableStateOf(userSettings?.jioCloudClientId ?: "") }
    var clientSecret by remember(userSettings) { mutableStateOf(userSettings?.jioCloudClientSecret ?: "") }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.DASHBOARD) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Admin & System Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CloudBridgePrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "ADMIN",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = CloudBridgePrimary
                            )
                        }
                    }
                    Text(
                        text = "Provider health, official adapter configuration, and system audit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Metrics Grid (Section 29)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Active Transfers",
                        value = "$activeJobs",
                        icon = Icons.Default.CloudSync,
                        iconColor = CloudBridgePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Completed",
                        value = "$completedJobs",
                        icon = Icons.Default.CheckCircle,
                        iconColor = CloudBridgeSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Jobs Run",
                        value = "$totalJobs",
                        icon = Icons.Default.CloudDone,
                        iconColor = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Failed",
                        value = "$failedJobs",
                        icon = Icons.Default.Error,
                        iconColor = if (failedJobs > 0) CloudBridgeError else Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Provider Health Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Provider Health Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ProviderHealthRow(
                        name = "Google Drive",
                        status = "Healthy (OAuth 2.0 connected, drive.file scope)",
                        isHealthy = true
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProviderHealthRow(
                        name = "JioCloud (Official)",
                        status = if (apiBaseUrl.isNotBlank()) "Configured with custom endpoint" else "Unconfigured (Pending official public API contract)",
                        isHealthy = apiBaseUrl.isNotBlank()
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProviderHealthRow(
                        name = "Mock JioCloud (Dev)",
                        status = if (isDevMode) "Active & operational for end-to-end simulations" else "Disabled in Production",
                        isHealthy = isDevMode
                    )
                }
            }
        }

        // Section 6 & 29: Official JioCloud Configuration
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
                        Text(
                            text = "Official JioCloud API Configuration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.Key, contentDescription = null, tint = CloudBridgePrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When JioCloud releases an official public API, input official gateway endpoints and OAuth client credentials here. The provider-adapter architecture activates automatically without requiring codebase restructuring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiBaseUrl,
                        onValueChange = { apiBaseUrl = it },
                        label = { Text("API Gateway Base URL") },
                        placeholder = { Text("https://api.jiocloud.com/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Client ID / App Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = clientSecret,
                        onValueChange = { clientSecret = it },
                        label = { Text("Client Secret") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.saveJioCloudConfig(apiBaseUrl, clientId, clientSecret)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Official Credentials")
                    }
                }
            }
        }

        // Section 35: Development Mock JioCloud Target Toggle
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Development Mock Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enables simulated JioCloud destination to validate folder preservation, chunk streaming, and verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDevMode,
                            onCheckedChange = { viewModel.toggleDevMode(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.triggerDemoTransfer() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger 14.8 GB Demo Pipeline (120 Files)")
                    }
                }
            }
        }

        // Section 29: System Audit Logs
        item {
            SectionHeader(
                title = "Audit Logs",
                subtitle = "Immutable ledger of all transfers, operations, and system events"
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No audit logs recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(auditLogs.take(10)) { log ->
                AuditLogRow(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProviderHealthRow(name: String, status: String, isHealthy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isHealthy) CloudBridgeSuccess else CloudBridgeWarning,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AuditLogRow(log: AuditLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${log.operation} • ${log.provider}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatTimestamp(log.timestamp)} • User: ${log.userId} • ${log.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (log.status == "COMPLETED") Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
            ) {
                Text(
                    text = log.status,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (log.status == "COMPLETED") CloudBridgeSuccess else Color(0xFF475569)
                )
            }
        }
    }
}
