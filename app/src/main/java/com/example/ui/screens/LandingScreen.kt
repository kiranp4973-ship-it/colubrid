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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CloudBridgeViewModel
import com.example.ui.Screen
import com.example.ui.components.CloudBridgeLogo
import com.example.ui.components.CloudBridgeTransferVisual
import com.example.ui.theme.CloudBridgeBorder
import com.example.ui.theme.CloudBridgePrimary

@Composable
fun LandingScreen(
    viewModel: CloudBridgeViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Top Bar Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CloudBridgeLogo(size = 34)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CloudBridge",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Move your files from cloud to cloud.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.AUTH) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign In")
                }
            }
        }

        // Hero Section
        item {
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
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CloudBridgePrimary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CloudBridgePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secure Cloud-to-Cloud Transfer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CloudBridgePrimary
                            )
                        }
                    }

                    Text(
                        text = "Move files between clouds.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Transfer your files securely without manually downloading and uploading everything. Direct cloud streaming, checksum validation, and folder hierarchy preservation.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.TRANSFER_WIZARD) },
                            colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Start Transfer")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.DASHBOARD) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dashboard")
                        }
                    }
                }
            }
        }

        // Visual Cloud to Cloud Transfer Diagram
        item {
            Column {
                Text(
                    text = "Transfer Pipeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CloudBridgeTransferVisual(
                    sourceName = "Google Drive",
                    destName = "JioCloud",
                    destSub = "Simulated / Configurable"
                )
                Text(
                    text = "Note: JioCloud integration uses a clean provider-adapter architecture. Development mode simulates JioCloud destination for full validation.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // 8 Feature Cards
        item {
            Text(
                text = "Key Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            val features = listOf(
                FeatureItem("Cloud-to-cloud transfer", "Stream files directly from Google Drive without hogging local device bandwidth.", Icons.Default.CloudSync),
                FeatureItem("Folder support", "Preserve recursive directory structures and nested paths end-to-end.", Icons.Default.FolderCopy),
                FeatureItem("Transfer progress", "Real-time metrics tracking transfer speed, ETA, and live bytes transferred.", Icons.Default.Speed),
                FeatureItem("Resume failed transfers", "Recover smoothly from network interruptions with checkpointing and backoff.", Icons.Default.Refresh),
                FeatureItem("Duplicate detection", "Choose between skipping existing files, replacing, or keeping both with auto-suffix.", Icons.Default.ContentCopy),
                FeatureItem("Transfer history", "Full audit trails and historical reports for every initiated cloud job.", Icons.Default.History),
                FeatureItem("Secure OAuth authentication", "OAuth 2.0 token isolation. We never ask for or store Google or cloud passwords.", Icons.Default.Lock),
                FeatureItem("No permanent file storage", "Your private files are never permanently stored on CloudBridge servers.", Icons.Default.CloudDone)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEach { feat ->
                    FeatureCard(feat)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Demo Transfer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Experience simulated 14.8 GB transfer from Google Drive to Mock JioCloud with real-time speed, pause/resume, and verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.triggerDemoTransfer() },
                        colors = ButtonDefaults.buttonColors(containerColor = CloudBridgePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run 14.8 GB Demo Transfer")
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

data class FeatureItem(val title: String, val description: String, val icon: ImageVector)

@Composable
fun FeatureCard(feature: FeatureItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CloudBridgePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = CloudBridgePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
