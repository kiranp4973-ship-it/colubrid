package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudFileType
import com.example.data.model.ConnectionStatus
import com.example.data.model.ItemStatus
import com.example.data.model.JobStatus
import com.example.data.model.ProviderType
import com.example.data.model.VerificationStatus
import com.example.ui.theme.CloudBridgeBorder
import com.example.ui.theme.CloudBridgeError
import com.example.ui.theme.CloudBridgeErrorContainer
import com.example.ui.theme.CloudBridgePrimary
import com.example.ui.theme.CloudBridgePrimaryContainer
import com.example.ui.theme.CloudBridgeSecondary
import com.example.ui.theme.CloudBridgeSuccess
import com.example.ui.theme.CloudBridgeSuccessContainer
import com.example.ui.theme.CloudBridgeWarning
import com.example.ui.theme.CloudBridgeWarningContainer
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudBridgeLogo(modifier: Modifier = Modifier, size: Int = 36) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CloudBridgePrimary),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = "CloudBridge",
                tint = Color.White,
                modifier = Modifier.size((size * 0.65).dp)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    iconColor: Color = CloudBridgePrimary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subValue != null) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: JobStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        JobStatus.COMPLETED -> Triple(CloudBridgeSuccessContainer, CloudBridgeSuccess, "COMPLETED")
        JobStatus.TRANSFERRING -> Triple(CloudBridgePrimaryContainer, CloudBridgePrimary, "TRANSFERRING")
        JobStatus.PREPARING -> Triple(CloudBridgePrimaryContainer, CloudBridgePrimary, "PREPARING")
        JobStatus.VERIFYING -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), "VERIFYING")
        JobStatus.QUEUED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "QUEUED")
        JobStatus.PAUSED -> Triple(CloudBridgeWarningContainer, CloudBridgeWarning, "PAUSED")
        JobStatus.FAILED -> Triple(CloudBridgeErrorContainer, CloudBridgeError, "FAILED")
        JobStatus.CANCELLED -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "CANCELLED")
        JobStatus.SKIPPED -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "SKIPPED")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = textColor
        )
    }
}

@Composable
fun ItemStatusBadge(status: ItemStatus, verification: VerificationStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when {
        verification == VerificationStatus.VERIFIED -> Triple(CloudBridgeSuccessContainer, CloudBridgeSuccess, "VERIFIED")
        status == ItemStatus.COMPLETED -> Triple(CloudBridgeSuccessContainer, CloudBridgeSuccess, "DONE")
        status == ItemStatus.TRANSFERRING -> Triple(CloudBridgePrimaryContainer, CloudBridgePrimary, "STREAMING")
        status == ItemStatus.VERIFYING -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), "VERIFYING")
        status == ItemStatus.FAILED -> Triple(CloudBridgeErrorContainer, CloudBridgeError, "FAILED")
        status == ItemStatus.SKIPPED -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "SKIPPED")
        status == ItemStatus.PAUSED -> Triple(CloudBridgeWarningContainer, CloudBridgeWarning, "PAUSED")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "QUEUED")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
            color = textColor
        )
    }
}

@Composable
fun ProviderStatusBadge(status: ConnectionStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        ConnectionStatus.CONNECTED -> Triple(CloudBridgeSuccessContainer, CloudBridgeSuccess, "Connected")
        ConnectionStatus.NOT_CONNECTED -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "Not connected")
        ConnectionStatus.NOT_CONFIGURED -> Triple(CloudBridgeWarningContainer, CloudBridgeWarning, "Not configured")
        ConnectionStatus.ERROR -> Triple(CloudBridgeErrorContainer, CloudBridgeError, "Error")
        ConnectionStatus.COMING_SOON -> Triple(Color(0xFFF1F5F9), Color(0xFF94A3B8), "Coming soon")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor
        )
    }
}

@Composable
fun FileTypeIcon(type: CloudFileType, modifier: Modifier = Modifier, size: Int = 22) {
    val (icon, color) = when (type) {
        CloudFileType.FOLDER -> Icons.Default.Folder to Color(0xFFF59E0B)
        CloudFileType.IMAGE -> Icons.Default.Image to Color(0xFF0284C7)
        CloudFileType.VIDEO -> Icons.Default.VideoFile to Color(0xFF8B5CF6)
        CloudFileType.AUDIO -> Icons.Default.AudioFile to Color(0xFFEC4899)
        CloudFileType.PDF -> Icons.Default.PictureAsPdf to Color(0xFFDC2626)
        CloudFileType.DOCUMENT -> Icons.Default.Description to Color(0xFF2563EB)
        CloudFileType.ARCHIVE -> Icons.Default.Archive to Color(0xFFD97706)
        CloudFileType.OTHER -> Icons.Default.Description to Color(0xFF64748B)
    }

    Icon(
        imageVector = icon,
        contentDescription = type.name,
        tint = color,
        modifier = modifier.size(size.dp)
    )
}

@Composable
fun CloudBridgeTransferVisual(
    modifier: Modifier = Modifier,
    sourceName: String = "Google Drive",
    destName: String = "JioCloud",
    destSub: String = "Configured / Mock"
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Google Drive
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Source",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Source",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Center: CloudBridge Bridge Node
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.2f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .weight(1f)
                            .background(CloudBridgePrimary.copy(alpha = 0.4f))
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CloudBridgePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure Bridge",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .weight(1f)
                            .background(CloudBridgePrimary.copy(alpha = 0.4f))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CloudBridge Engine",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CloudBridgePrimary
                )
                Text(
                    text = "OAuth • Streaming",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: JioCloud
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Destination",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = destName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = destSub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return "${DecimalFormat("#,##0.#").format(value)} ${units[digitGroups]}"
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 MB/s"
    val mbps = bytesPerSec / (1024.0 * 1024.0)
    return "${DecimalFormat("#0.0").format(mbps)} MB/s"
}

fun formatTimestamp(timeMs: Long): String {
    if (timeMs <= 0) return "—"
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

@Composable
fun RealtimeProtocolBadge(
    protocol: com.example.data.realtime.RealtimeProtocol,
    connectionStatus: com.example.data.realtime.RealtimeConnectionStatus,
    pollingIntervalMs: Long = 1000L,
    modifier: Modifier = Modifier
) {
    val (dotColor, statusText) = when (connectionStatus) {
        com.example.data.realtime.RealtimeConnectionStatus.CONNECTED -> Color(0xFF10B981) to "Connected"
        com.example.data.realtime.RealtimeConnectionStatus.CONNECTING -> Color(0xFFF59E0B) to "Connecting"
        com.example.data.realtime.RealtimeConnectionStatus.POLLING_FALLBACK -> Color(0xFF3B82F6) to "Fallback (${pollingIntervalMs}ms)"
        com.example.data.realtime.RealtimeConnectionStatus.RECONNECTING -> Color(0xFFF59E0B) to "Reconnecting"
        com.example.data.realtime.RealtimeConnectionStatus.ERROR -> Color(0xFFEF4444) to "Error"
        com.example.data.realtime.RealtimeConnectionStatus.DISCONNECTED -> Color(0xFF9CA3AF) to "Offline"
    }

    val (bg, textColor) = when (protocol) {
        com.example.data.realtime.RealtimeProtocol.WEBSOCKET -> Color(0xFFEEF2FF) to Color(0xFF4338CA)
        com.example.data.realtime.RealtimeProtocol.SSE -> Color(0xFFECFDF5) to Color(0xFF047857)
        com.example.data.realtime.RealtimeProtocol.POLLING -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "${protocol.shortName} • $statusText",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

