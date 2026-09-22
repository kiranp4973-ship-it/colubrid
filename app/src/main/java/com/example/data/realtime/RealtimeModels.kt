package com.example.data.realtime

import com.example.data.model.LogLevel
import java.util.UUID

enum class RealtimeProtocol(
    val title: String,
    val shortName: String,
    val scheme: String,
    val description: String
) {
    WEBSOCKET(
        title = "WebSocket (Live Stream)",
        shortName = "WebSocket",
        scheme = "ws://",
        description = "Full-duplex persistent socket stream with binary chunk metrics and heartbeat pings"
    ),
    SSE(
        title = "Server-Sent Events (SSE)",
        shortName = "SSE",
        scheme = "http://",
        description = "Unidirectional HTTP text/event-stream with auto-reconnect and monotonic event IDs"
    ),
    POLLING(
        title = "Polling Fallback (HTTP GET)",
        shortName = "Polling",
        scheme = "http://",
        description = "Configurable periodic HTTP polling with conditional delta sync & low overhead"
    )
}

enum class RealtimeConnectionStatus(val title: String, val colorHex: Long) {
    CONNECTED("Connected", 0xFF10B981),
    CONNECTING("Connecting...", 0xFFF59E0B),
    RECONNECTING("Reconnecting...", 0xFFF97316),
    POLLING_FALLBACK("Polling Active", 0xFF3B82F6),
    DISCONNECTED("Disconnected", 0xFF6B7280),
    ERROR("Connection Error", 0xFFEF4444)
}

data class RealtimeTransferProgress(
    val jobId: String,
    val filesCompleted: Int,
    val filesRemaining: Int,
    val totalFiles: Int,
    val failedFiles: Int = 0,
    val skippedFiles: Int = 0,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val currentFileName: String? = null,
    val currentFileBytes: Long = 0L,
    val currentFileTotalBytes: Long = 0L,
    val transferSpeedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val activeStreams: Int = 1,
    val protocol: RealtimeProtocol = RealtimeProtocol.WEBSOCKET,
    val connectionStatus: RealtimeConnectionStatus = RealtimeConnectionStatus.CONNECTED,
    val latencyMs: Long = 14L,
    val timestamp: Long = System.currentTimeMillis(),
    val isFallbackActive: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val currentFileFraction: Float
        get() = if (currentFileTotalBytes > 0) (currentFileBytes.toFloat() / currentFileTotalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val currentFilePercent: Int
        get() = (currentFileFraction * 100).toInt()

    val etaFormatted: String
        get() {
            if (etaSeconds <= 0) return if (filesCompleted == totalFiles && totalFiles > 0) "Completed" else "Calculating..."
            val min = etaSeconds / 60
            val sec = etaSeconds % 60
            return if (min > 0) "${min}m ${sec}s" else "${sec}s"
        }
}

data class RealtimeConnectionLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val protocol: RealtimeProtocol,
    val level: LogLevel,
    val message: String,
    val details: String? = null
)
