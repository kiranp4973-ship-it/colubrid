package com.example.data.repository

import com.example.data.engine.TransferEngine
import com.example.data.local.AuditLogEntity
import com.example.data.local.CloudBridgeDatabase
import com.example.data.local.ProviderConnectionEntity
import com.example.data.local.TransferEventEntity
import com.example.data.local.TransferItemEntity
import com.example.data.local.TransferJobEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.model.CloudFile
import com.example.data.model.ConflictStrategy
import com.example.data.model.ConnectionStatus
import com.example.data.model.ItemStatus
import com.example.data.model.JobStatus
import com.example.data.model.ProviderType
import com.example.data.model.UserRole
import com.example.data.provider.CloudProvider
import com.example.data.provider.DropboxProvider
import com.example.data.provider.FileListResult
import com.example.data.provider.GoogleDriveProvider
import com.example.data.provider.JioCloudProvider
import com.example.data.provider.MockJioCloudProvider
import com.example.data.provider.OneDriveProvider
import com.example.data.provider.ProviderResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CloudBridgeRepository(private val db: CloudBridgeDatabase) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Providers
    val googleDriveProvider = GoogleDriveProvider()
    val jioCloudProvider = JioCloudProvider()
    val mockJioCloudProvider = MockJioCloudProvider()
    val oneDriveProvider = OneDriveProvider()
    val dropboxProvider = DropboxProvider()

    // Engine
    val transferEngine = TransferEngine(
        transferJobDao = db.transferJobDao(),
        transferItemDao = db.transferItemDao(),
        transferEventDao = db.transferEventDao(),
        auditLogDao = db.auditLogDao()
    )

    // Current Session User
    private val defaultUser = UserEntity(
        id = "usr_prod_demo_01",
        email = "user@cloudbridge.io",
        displayName = "Alex Vance",
        role = UserRole.ADMIN
    )
    private val _currentUser = MutableStateFlow<UserEntity?>(defaultUser)
    val currentUser = _currentUser.asStateFlow()

    // Development Mode State (allows simulated JioCloud destination)
    private val _isDevMode = MutableStateFlow(true)
    val isDevMode = _isDevMode.asStateFlow()

    init {
        // Initialize default user session
        repoScope.launch {
            try {
                db.userDao().insertUser(defaultUser)
            _currentUser.value = defaultUser

            // Initialize default connections
            db.providerConnectionDao().insertOrUpdate(
                ProviderConnectionEntity(
                    id = "${defaultUser.id}_GOOGLE_DRIVE",
                    userId = defaultUser.id,
                    provider = ProviderType.GOOGLE_DRIVE,
                    status = ConnectionStatus.CONNECTED,
                    accountEmail = "user.cloudbridge@gmail.com",
                    accountName = "Alex's Google Drive",
                    storageUsedBytes = 28_400_000_000L,
                    storageTotalBytes = 100_000_000_000L,
                    connectedAt = System.currentTimeMillis() - 86400000L
                )
            )

            db.providerConnectionDao().insertOrUpdate(
                ProviderConnectionEntity(
                    id = "${defaultUser.id}_JIO_CLOUD",
                    userId = defaultUser.id,
                    provider = ProviderType.JIO_CLOUD,
                    status = ConnectionStatus.NOT_CONFIGURED,
                    statusMessage = "JioCloud connection is not configured yet."
                )
            )

            db.providerConnectionDao().insertOrUpdate(
                ProviderConnectionEntity(
                    id = "${defaultUser.id}_ONEDRIVE",
                    userId = defaultUser.id,
                    provider = ProviderType.ONEDRIVE,
                    status = ConnectionStatus.COMING_SOON
                )
            )

            db.providerConnectionDao().insertOrUpdate(
                ProviderConnectionEntity(
                    id = "${defaultUser.id}_DROPBOX",
                    userId = defaultUser.id,
                    provider = ProviderType.DROPBOX,
                    status = ConnectionStatus.COMING_SOON
                )
            )

            // Clean up any interrupted transfers from previous app lifecycle
            val interruptedJobs = db.transferJobDao().getRecentJobsOnce(100)
                .filter { it.status == JobStatus.TRANSFERRING || it.status == JobStatus.PREPARING }
            for (job in interruptedJobs) {
                db.transferJobDao().updateJob(job.copy(status = JobStatus.PAUSED, errorMessage = "Transfer paused due to app closure."))
                val items = db.transferItemDao().getItemsForJobOnce(job.id)
                for (item in items) {
                    if (item.status == ItemStatus.TRANSFERRING) {
                        db.transferItemDao().updateItem(item.copy(status = ItemStatus.PAUSED))
                    }
                }
            }

            // Initialize default settings
            db.userSettingsDao().insertOrUpdate(
                UserSettingsEntity(
                    userId = defaultUser.id,
                    defaultConflictStrategy = ConflictStrategy.SKIP_DUPLICATE,
                    maxConcurrentTransfers = 3,
                    devModeEnabled = true
                )
            )
            } catch (e: Exception) {
                // Handled during teardown / cancellation
            }
        }
    }

    fun getDestinationProvider(): CloudProvider {
        return if (_isDevMode.value) {
            mockJioCloudProvider
        } else {
            jioCloudProvider
        }
    }

    // User authentication & session
    fun signIn(email: String, role: UserRole = UserRole.USER) {
        val user = UserEntity(
            id = "usr_" + email.hashCode().coerceAtLeast(1),
            email = email,
            displayName = email.substringBefore("@").replace(".", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            role = role
        )
        repoScope.launch {
            db.userDao().insertUser(user)
            _currentUser.value = user
        }
    }

    fun signOut() {
        _currentUser.value = null
    }

    fun toggleDevMode(enabled: Boolean) {
        _isDevMode.value = enabled
        val user = _currentUser.value ?: return
        repoScope.launch {
            val settings = db.userSettingsDao().insertOrUpdate(
                UserSettingsEntity(userId = user.id, devModeEnabled = enabled)
            )
        }
    }

    // Provider connections
    fun getConnections(): Flow<List<ProviderConnectionEntity>> {
        val userId = _currentUser.value?.id ?: "usr_prod_demo_01"
        return db.providerConnectionDao().getConnectionsForUser(userId)
    }

    suspend fun connectGoogleDrive(): ProviderResult<Unit> {
        val result = googleDriveProvider.connect()
        if (result is ProviderResult.Success) {
            val user = _currentUser.value ?: return ProviderResult.Error("No active user")
            db.providerConnectionDao().insertOrUpdate(
                ProviderConnectionEntity(
                    id = "${user.id}_GOOGLE_DRIVE",
                    userId = user.id,
                    provider = ProviderType.GOOGLE_DRIVE,
                    status = ConnectionStatus.CONNECTED,
                    accountEmail = result.data.email,
                    accountName = result.data.name,
                    storageUsedBytes = result.data.storageUsedBytes,
                    storageTotalBytes = result.data.storageTotalBytes,
                    connectedAt = System.currentTimeMillis()
                )
            )
            return ProviderResult.Success(Unit)
        }
        return ProviderResult.Error("Could not connect Google Drive")
    }

    suspend fun disconnectGoogleDrive() {
        googleDriveProvider.disconnect()
        val user = _currentUser.value ?: return
        db.providerConnectionDao().deleteConnection(user.id, ProviderType.GOOGLE_DRIVE.name)
    }

    // File Browser
    suspend fun listDriveFiles(folderId: String? = null, query: String? = null): ProviderResult<FileListResult> {
        return googleDriveProvider.listFiles(folderId, query)
    }

    // Transfer Creation
    suspend fun createAndStartTransferJob(
        selectedFiles: List<CloudFile>,
        destinationPath: String,
        conflictStrategy: ConflictStrategy
    ): String {
        val user = _currentUser.value ?: throw IllegalStateException("Not authenticated")
        val jobId = "job_" + System.currentTimeMillis()

        // Recursively expand any selected folders
        val itemsToTransfer = mutableListOf<TransferItemEntity>()
        var totalBytes = 0L

        for (file in selectedFiles) {
            if (file.isFolder) {
                // Add the folder entity
                itemsToTransfer.add(
                    TransferItemEntity(
                        id = "item_${jobId}_${file.id}",
                        jobId = jobId,
                        sourceFileId = file.id,
                        sourcePath = file.parentId ?: "ROOT",
                        destinationPath = destinationPath,
                        fileName = file.name,
                        mimeType = file.mimeType,
                        fileSize = 0L,
                        isFolder = true
                    )
                )
                // Discover all descendant files
                val descendants = googleDriveProvider.getAllDescendantFiles(file.id)
                for (desc in descendants) {
                    if (!desc.isFolder) totalBytes += desc.size
                    itemsToTransfer.add(
                        TransferItemEntity(
                            id = "item_${jobId}_${desc.id}",
                            jobId = jobId,
                            sourceFileId = desc.id,
                            sourcePath = desc.parentId ?: file.id,
                            destinationPath = destinationPath,
                            fileName = desc.name,
                            mimeType = desc.mimeType,
                            fileSize = desc.size,
                            isFolder = desc.isFolder,
                            checksum = desc.checksum
                        )
                    )
                }
            } else {
                totalBytes += file.size
                itemsToTransfer.add(
                    TransferItemEntity(
                        id = "item_${jobId}_${file.id}",
                        jobId = jobId,
                        sourceFileId = file.id,
                        sourcePath = file.parentId ?: "ROOT",
                        destinationPath = destinationPath,
                        fileName = file.name,
                        mimeType = file.mimeType,
                        fileSize = file.size,
                        isFolder = false,
                        checksum = file.checksum
                    )
                )
            }
        }

        val destinationProvider = getDestinationProvider()

        val job = TransferJobEntity(
            id = jobId,
            userId = user.id,
            sourceProvider = ProviderType.GOOGLE_DRIVE,
            destinationProvider = destinationProvider.providerType,
            destinationPath = destinationPath,
            conflictStrategy = conflictStrategy,
            status = JobStatus.QUEUED,
            totalFiles = itemsToTransfer.filter { !it.isFolder }.size,
            totalBytes = totalBytes
        )

        db.transferJobDao().insertJob(job)
        db.transferItemDao().insertItems(itemsToTransfer)

        // Launch execution in TransferEngine
        transferEngine.startJob(
            job = job,
            sourceProvider = googleDriveProvider,
            destinationProvider = destinationProvider,
            maxConcurrency = 3
        )

        return jobId
    }

    // Demo Mode Transfer generator (Section 45)
    suspend fun createDemoTransfer(): String {
        val user = _currentUser.value ?: throw IllegalStateException("Not authenticated")
        val jobId = "job_demo_" + System.currentTimeMillis()

        val sampleFiles = listOf(
            Triple("photo_raw_gallery.zip", 2_400_000_000L, "application/zip"),
            Triple("company_keynote_4k.mp4", 5_800_000_000L, "video/mp4"),
            Triple("cloud_architecture_v2.pdf", 450_000_000L, "application/pdf"),
            Triple("product_asset_pack.tar.gz", 3_200_000_000L, "application/gzip"),
            Triple("database_snapshot_2026.sql", 1_950_000_000L, "application/sql"),
            Triple("team_allhands_q3.mov", 1_000_000_000L, "video/quicktime")
        )

        val totalBytes = sampleFiles.sumOf { it.second }
        val items = sampleFiles.mapIndexed { idx, (name, size, mime) ->
            TransferItemEntity(
                id = "demo_item_${jobId}_$idx",
                jobId = jobId,
                sourceFileId = "src_demo_$idx",
                sourcePath = "ROOT",
                destinationPath = "/CloudBridge Transfers/Demo Archive/",
                fileName = name,
                mimeType = mime,
                fileSize = size,
                status = ItemStatus.QUEUED
            )
        }

        val job = TransferJobEntity(
            id = jobId,
            userId = user.id,
            sourceProvider = ProviderType.GOOGLE_DRIVE,
            destinationProvider = ProviderType.JIO_CLOUD,
            destinationPath = "/CloudBridge Transfers/Demo Archive/",
            conflictStrategy = ConflictStrategy.SKIP_DUPLICATE,
            status = JobStatus.QUEUED,
            totalFiles = items.size,
            totalBytes = totalBytes
        )

        db.transferJobDao().insertJob(job)
        db.transferItemDao().insertItems(items)

        transferEngine.startJob(
            job = job,
            sourceProvider = googleDriveProvider,
            destinationProvider = mockJioCloudProvider,
            maxConcurrency = 3
        )

        return jobId
    }

    // Transfer observation
    fun getTransferJobs(): Flow<List<TransferJobEntity>> {
        val userId = _currentUser.value?.id ?: "usr_prod_demo_01"
        return db.transferJobDao().getJobsForUser(userId)
    }

    fun getTransferJob(jobId: String): Flow<TransferJobEntity?> {
        return db.transferJobDao().getJobById(jobId)
    }

    fun getTransferItems(jobId: String): Flow<List<TransferItemEntity>> {
        return db.transferItemDao().getItemsForJob(jobId)
    }

    fun getTransferEvents(jobId: String): Flow<List<TransferEventEntity>> {
        return db.transferEventDao().getEventsForJob(jobId)
    }

    fun getSettings(): Flow<UserSettingsEntity?> {
        val userId = _currentUser.value?.id ?: "usr_prod_demo_01"
        return db.userSettingsDao().getSettings(userId)
    }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        db.userSettingsDao().insertOrUpdate(settings)
        if (settings.jioCloudApiBaseUrl.isNotBlank()) {
            jioCloudProvider.configureOfficialCredentials(
                settings.jioCloudApiBaseUrl,
                settings.jioCloudClientId,
                settings.jioCloudClientSecret
            )
        }
    }

    fun getAuditLogs(): Flow<List<AuditLogEntity>> {
        return db.auditLogDao().getRecentLogs()
    }

    fun getTotalJobsCount() = db.transferJobDao().getTotalJobsCount()
    fun getCompletedJobsCount() = db.transferJobDao().getCompletedJobsCount()
    fun getFailedJobsCount() = db.transferJobDao().getFailedJobsCount()
    fun getActiveJobsCount() = db.transferJobDao().getActiveJobsCount()
    fun getUserCount() = db.userDao().getUserCount()

    suspend fun deleteJob(jobId: String) {
        db.transferJobDao().deleteJob(jobId)
        db.transferItemDao().deleteItemsForJob(jobId)
    }

    fun shutdown() {
        repoScope.cancel()
        transferEngine.shutdown()
    }
}
