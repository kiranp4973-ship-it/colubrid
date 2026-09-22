package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ConflictStrategy
import com.example.data.model.ConnectionStatus
import com.example.data.model.ItemStatus
import com.example.data.model.JobStatus
import com.example.data.model.LogLevel
import com.example.data.model.ProviderType
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val role: UserRole = UserRole.USER,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "provider_connections")
data class ProviderConnectionEntity(
    @PrimaryKey val id: String, // e.g. "user1_GOOGLE_DRIVE"
    val userId: String,
    val provider: ProviderType,
    val status: ConnectionStatus,
    val accountEmail: String? = null,
    val accountName: String? = null,
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val connectedAt: Long? = null,
    val lastSyncAt: Long? = null,
    val statusMessage: String? = null
)

@Entity(tableName = "transfer_jobs")
data class TransferJobEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceProvider: ProviderType,
    val destinationProvider: ProviderType,
    val destinationPath: String = "/CloudBridge Transfers/",
    val conflictStrategy: ConflictStrategy = ConflictStrategy.SKIP_DUPLICATE,
    val status: JobStatus = JobStatus.QUEUED,
    val totalFiles: Int = 0,
    val completedFiles: Int = 0,
    val failedFiles: Int = 0,
    val skippedFiles: Int = 0,
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val currentSpeedBytesPerSec: Long = 0L,
    val errorMessage: String? = null
)

@Entity(tableName = "transfer_items")
data class TransferItemEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val sourceFileId: String,
    val sourcePath: String,
    val destinationPath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val isFolder: Boolean = false,
    val status: ItemStatus = ItemStatus.QUEUED,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val bytesTransferred: Long = 0L,
    val percentage: Int = 0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val error: String? = null,
    val retryCount: Int = 0,
    val checksum: String? = null
)

@Entity(tableName = "transfer_events")
data class TransferEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val jobId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
    val defaultConflictStrategy: ConflictStrategy = ConflictStrategy.SKIP_DUPLICATE,
    val maxConcurrentTransfers: Int = 3,
    val autoRetryFailed: Boolean = true,
    val darkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val devModeEnabled: Boolean = true, // Enables simulated JioCloud destination in dev
    val jioCloudApiBaseUrl: String = "",
    val jioCloudClientId: String = "",
    val jioCloudClientSecret: String = "",
    val jioCloudAuthEndpoint: String = "",
    val realtimeProtocol: String = "WEBSOCKET",
    val pollingIntervalMs: Long = 1000L,
    val realtimeFallbackEnabled: Boolean = true
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val jobId: String? = null,
    val userId: String,
    val provider: String,
    val operation: String,
    val status: String,
    val errorCode: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L
)
