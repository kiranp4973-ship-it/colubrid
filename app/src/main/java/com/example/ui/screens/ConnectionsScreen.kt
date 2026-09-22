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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.data.model.ProviderType
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.ProviderStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CloudBridgeError
import com.example.ui.theme.CloudBridgePrimary
import com.example.ui.theme.CloudBridgeSuccess
import com.example.ui.theme.CloudBridgeWarning

@Composable
fun ConnectionsScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val connections by viewModel.providerConnections.collectAsState()
    val isDevMode by viewModel.isDevMode.collectAsState()

    val googleDriveConn = connections.find { it.provider == ProviderType.GOOGLE_DRIVE }
    val jioConn = connections.find { it.provider == ProviderType.JIO_CLOUD }
    val oneDriveConn = connections.find { it.provider == ProviderType.ONEDRIVE }
    val dropboxConn = connections.find { it.provider == ProviderType.DROPBOX }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Cloud Connections",
                subtitle = "Manage authorized cloud storage providers via secure OAuth"
            )
        }

        // Security Notice Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CloudBridgePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CloudBridge connects using standard OAuth 2.0. We never receive or store your Google or cloud passwords. Scopes are strictly limited to files you transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Google Drive Provider Card
        item {
            val isGConnected = googleDriveConn?.status == ConnectionStatus.CONNECTED
            CloudProviderConnectionCard(
                providerName = "Google Drive",
                description = "Primary Source Cloud. Browse My Drive, folders, files, and stream downloads directly.",
                icon = Icons.Default.CloudDownload,
                iconColor = Color(0xFF0284C7),
                status = googleDriveConn?.status ?: ConnectionStatus.CONNECTED,
                accountEmail = googleDriveConn?.accountEmail ?: "user.cloudbridge@gmail.com",
                storageInfo = if (isGConnected) "28.4 GB used of 100 GB" else null,
                actionButton = {
                    if (isGConnected) {
                        OutlinedButton(
                            onClick = { viewModel.disconnectGoogleDrive() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CloudBridgeError)
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disconnect")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.connectGoogleDrive() },
                            colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect via Google")
                        }
                    }
                }
            )
        }

        // JioCloud Provider Card
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
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "JioCloud / JioAICloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Primary Destination Cloud (Adapter Architecture)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isDevMode) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                Text(
                                    text = "Dev Mock Mode",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CloudBridgeSuccess
                                )
                            }
                        } else {
                            ProviderStatusBadge(status = ConnectionStatus.NOT_CONFIGURED)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isDevMode) {
                            "Development mode is active with simulated JioCloud destination. Full transfer engine, chunking, and file verification can be tested immediately."
                        } else {
                            "JioCloud connection is not configured yet. An official public API endpoint and developer credentials are required before production connection."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isDevMode,
                                onCheckedChange = { viewModel.toggleDevMode(it) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulated Dev Target",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedButton(onClick = { viewModel.navigateTo(Screen.ADMIN) }) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure API")
                        }
                    }
                }
            }
        }

        // OneDrive Provider Card (Coming soon)
        item {
            CloudProviderConnectionCard(
                providerName = "Microsoft OneDrive",
                description = "Integration roadmap: Support for OneDrive personal and business libraries.",
                icon = Icons.Default.Cloud,
                iconColor = Color(0xFF0078D4),
                status = ConnectionStatus.COMING_SOON,
                actionButton = {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text("Coming soon")
                    }
                }
            )
        }

        // Dropbox Provider Card (Coming soon)
        item {
            CloudProviderConnectionCard(
                providerName = "Dropbox",
                description = "Integration roadmap: Support for Dropbox shared spaces and folders.",
                icon = Icons.Default.Storage,
                iconColor = Color(0xFF0061FE),
                status = ConnectionStatus.COMING_SOON,
                actionButton = {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text("Coming soon")
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CloudProviderConnectionCard(
    providerName: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    accountEmail: String? = null,
    storageInfo: String? = null,
    actionButton: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = providerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (accountEmail != null) {
                            Text(text = accountEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                ProviderStatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (storageInfo != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = storageInfo, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = CloudBridgePrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                actionButton()
            }
        }
    }
}
