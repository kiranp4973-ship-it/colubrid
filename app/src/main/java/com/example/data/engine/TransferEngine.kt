package com.example.data.engine

import com.example.data.local.AuditLogDao
import com.example.data.local.AuditLogEntity
import com.example.data.local.TransferEventDao
import com.example.data.local.TransferEventEntity
import com.example.data.local.TransferItemDao
import com.example.data.local.TransferItemEntity
import com.example.data.local.TransferJobDao
import com.example.data.local.TransferJobEntity
import com.example.data.model.CloudFile
import com.example.data.model.ConflictStrategy
import com.example.data.model.ItemStatus
import com.example.data.model.JobStatus
import com.example.data.model.LogLevel
import com.example.data.model.ProviderType
import com.example.data.model.VerificationStatus
import com.example.data.provider.CloudProvider
import com.example.data.provider.ProviderResult
import com.example.data.realtime.RealtimeTransferWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

class TransferEngine(
    private val transferJobDao: TransferJobDao,
    private val transferItemDao: TransferItemDao,
    private val transferEventDao: TransferEventDao,
    private val auditLogDao: AuditLogDao,
    val realtimeWorker: RealtimeTransferWorker = RealtimeTransferWorker()
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeExecutionJobs = ConcurrentHashMap<String, Job>()
    private val pausedJobIds = ConcurrentHashMap.newKeySet<String>()

    private val _engineSpeedBytesPerSec = MutableStateFlow(0L)
    val engineSpeedBytesPerSec = _engineSpeedBytesPerSec.asStateFlow()

    fun startJob(
        job: TransferJobEntity,
        sourceProvider: CloudProvider,
        destinationProvider: CloudProvider,
        maxConcurrency: Int = 3
    ): Job {
        val coroutineJob = engineScope.launch {
            runTransferProcess(job, sourceProvider, destinationProvider, maxConcurrency)
        }
        activeExecutionJobs[job.id] = coroutineJob
        return coroutineJob
    }

    fun getActiveExecutionJob(jobId: String): Job? = activeExecutionJobs[jobId]

    fun shutdown() {
        engineScope.cancel()
        realtimeWorker.shutdown()
    }

    fun pauseJob(jobId: String) {
        pausedJobIds.add(jobId)
        activeExecutionJobs[jobId]?.cancel()
        activeExecutionJobs.remove(jobId)
        engineScope.launch {
            transferJobDao.updateJobStatus(jobId, JobStatus.PAUSED)
            val items = transferItemDao.getItemsForJobOnce(jobId)
            for (item in items) {
                if (item.status == ItemStatus.TRANSFERRING || item.status == ItemStatus.QUEUED) {
                    transferItemDao.updateItem(item.copy(status = ItemStatus.PAUSED))
                }
            }
            logEvent(jobId, "Transfer job paused by user", LogLevel.INFO)
        }
    }

    fun resumeJob(
        jobId: String,
        sourceProvider: CloudProvider,
        destinationProvider: CloudProvider,
        maxConcurrency: Int = 3
    ) {
        pausedJobIds.remove(jobId)
        engineScope.launch {
            val job = transferJobDao.getJobByIdOnce(jobId)
            if (job != null) {
                val items = transferItemDao.getItemsForJobOnce(jobId)
                for (item in items) {
                    if (item.status == ItemStatus.PAUSED) {
                        transferItemDao.updateItem(item.copy(status = ItemStatus.QUEUED))
                    }
                }
                transferJobDao.updateJobStatus(jobId, JobStatus.QUEUED)
                logEvent(jobId, "Transfer job resumed", LogLevel.INFO)
                startJob(job, sourceProvider, destinationProvider, maxConcurrency)
            }
        }
    }

    fun cancelJob(jobId: String) {
        pausedJobIds.remove(jobId)
        activeExecutionJobs[jobId]?.cancel()
        activeExecutionJobs.remove(jobId)
        engineScope.launch {
            transferJobDao.updateJobStatus(jobId, JobStatus.CANCELLED)
            val items = transferItemDao.getItemsForJobOnce(jobId)
            for (item in items) {
                if (item.status == ItemStatus.TRANSFERRING || item.status == ItemStatus.QUEUED || item.status == ItemStatus.PAUSED) {
                    transferItemDao.updateItem(item.copy(status = ItemStatus.CANCELLED))
                }
            }
            logEvent(jobId, "Transfer job cancelled by user", LogLevel.WARN)
        }
    }

    fun retryFailedItems(
        jobId: String,
        sourceProvider: CloudProvider,
        destinationProvider: CloudProvider,
        maxConcurrency: Int = 3
    ) {
        engineScope.launch {
            val job = transferJobDao.getJobByIdOnce(jobId) ?: return@launch
            val items = transferItemDao.getItemsForJobOnce(jobId)
            val failedItems = items.filter { it.status == ItemStatus.FAILED }
            for (item in failedItems) {
                transferItemDao.updateItem(
                    item.copy(
                        status = ItemStatus.QUEUED,
                        error = null,
                        retryCount = item.retryCount + 1,
                        percentage = 0,
                        bytesTransferred = 0L
                    )
                )
            }
            transferJobDao.updateJob(
                job.copy(
                    status = JobStatus.QUEUED,
                    failedFiles = 0,
                    errorMessage = null
                )
            )
            logEvent(jobId, "Retrying ${failedItems.size} failed files", LogLevel.INFO)
            startJob(job, sourceProvider, destinationProvider, maxConcurrency)
        }
    }

    private suspend fun runTransferProcess(
        job: TransferJobEntity,
        sourceProvider: CloudProvider,
        destinationProvider: CloudProvider,
        maxConcurrency: Int
    ) {
        val startTime = System.currentTimeMillis()
        try {
            // STEP 1: PREPARING
            transferJobDao.updateJob(
                job.copy(
                    status = JobStatus.PREPARING,
                    startedAt = startTime,
                    errorMessage = null
                )
            )
            logEvent(job.id, "Transfer engine initialized. Target destination: ${job.destinationPath}", LogLevel.INFO)

            // Ensure destination root folder exists if not root
            val trimmedDest = job.destinationPath.trim('/')
            val destRootFolderId = if (trimmedDest.isEmpty()) {
                null
            } else {
                val rootCreateResult = destinationProvider.createFolder(
                    name = trimmedDest,
                    parentFolderId = null
                )
                when (rootCreateResult) {
                    is ProviderResult.Success -> rootCreateResult.data.id
                    is ProviderResult.Error -> null
                }
            }

            // Retrieve items to transfer
            val allItems = transferItemDao.getItemsForJobOnce(job.id)
            if (allItems.isEmpty()) {
                transferJobDao.updateJob(
                    job.copy(
                        status = JobStatus.COMPLETED,
                        completedAt = System.currentTimeMillis()
                    )
                )
                logEvent(job.id, "No files found to transfer. Completed immediately.", LogLevel.INFO)
                return
            }

            // Folder structure mapping: sourceFolderId -> destFolderId
            val folderMap = ConcurrentHashMap<String, String>()
            destRootFolderId?.let { folderMap["ROOT"] = it }

            // STEP 2: TRANSFERRING
            transferJobDao.updateJobStatus(job.id, JobStatus.TRANSFERRING)
            logEvent(job.id, "Beginning transfer of ${allItems.size} items with concurrency limit $maxConcurrency", LogLevel.INFO)

            val semaphore = Semaphore(maxConcurrency)
            val fileItems = allItems.filter { !it.isFolder && it.status != ItemStatus.COMPLETED && it.status != ItemStatus.SKIPPED }
            val folderItems = allItems.filter { it.isFolder && it.status != ItemStatus.COMPLETED }

            // Create folders first to preserve structure
            for (folderItem in folderItems) {
                val parentDestId = folderMap[folderItem.sourcePath] ?: destRootFolderId
                val createResult = destinationProvider.createFolder(folderItem.fileName, parentDestId)
                if (createResult is ProviderResult.Success) {
                    folderMap[folderItem.sourceFileId] = createResult.data.id
                    transferItemDao.updateItem(
                        folderItem.copy(
                            status = ItemStatus.COMPLETED,
                            verificationStatus = VerificationStatus.VERIFIED,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            var completedCount = allItems.count { !it.isFolder && it.status == ItemStatus.COMPLETED }
            var failedCount = 0
            var skippedCount = allItems.count { it.status == ItemStatus.SKIPPED }
            var totalTransferredBytes = allItems.filter { !it.isFolder && it.status == ItemStatus.COMPLETED }.sumOf { it.fileSize }
            val totalBytes = job.totalBytes
            val totalFiles = allItems.count { !it.isFolder }

            val initialRemaining = maxOf(0, totalFiles - completedCount - skippedCount)
            realtimeWorker.pushProgressUpdate(
                jobId = job.id,
                filesCompleted = completedCount,
                filesRemaining = initialRemaining,
                totalFiles = totalFiles,
                failedFiles = 0,
                skippedFiles = skippedCount,
                bytesTransferred = totalTransferredBytes,
                totalBytes = totalBytes,
                currentFileName = if (fileItems.isNotEmpty()) fileItems.first().fileName else "Initializing pipeline...",
                currentFileBytes = 0L,
                currentFileTotalBytes = if (fileItems.isNotEmpty()) fileItems.first().fileSize else 0L,
                transferSpeedBytesPerSec = 0L,
                etaSeconds = 0L
            )

            // Parallel file transfers with rate control and chunking
            val transferJobs = fileItems.map { item ->
                engineScope.launch {
                    semaphore.withPermit {
                        if (pausedJobIds.contains(job.id)) return@withPermit

                        val fileStart = System.currentTimeMillis()
                        try {
                            transferItemDao.updateItem(
                                item.copy(
                                    status = ItemStatus.TRANSFERRING,
                                    startedAt = fileStart
                                )
                            )

                            // 1. Duplicate Detection
                            val parentDestId = folderMap[item.sourcePath] ?: destRootFolderId
                            val checkResult = destinationProvider.checkFileExists(
                                name = item.fileName,
                                parentFolderId = parentDestId,
                                checksum = item.checksum,
                                size = item.fileSize
                            )

                            var targetFileName = item.fileName
                            var shouldSkip = false

                            if (checkResult is ProviderResult.Success && checkResult.data.exists) {
                                when (job.conflictStrategy) {
                                    ConflictStrategy.SKIP_DUPLICATE -> {
                                        shouldSkip = true
                                        logEvent(job.id, "Skipped duplicate file: ${item.fileName}", LogLevel.INFO)
                                    }
                                    ConflictStrategy.REPLACE -> {
                                        // Overwrite will be handled by upload
                                        logEvent(job.id, "Overwriting existing file: ${item.fileName}", LogLevel.INFO)
                                    }
                                    ConflictStrategy.KEEP_BOTH -> {
                                        val extension = item.fileName.substringAfterLast(".", "")
                                        val base = if (item.fileName.contains(".") && !item.fileName.startsWith(".")) {
                                            item.fileName.substringBeforeLast(".")
                                        } else {
                                            item.fileName
                                        }
                                        var counter = 1
                                        var candidateName = item.fileName
                                        do {
                                            candidateName = if (extension.isNotEmpty() && base != item.fileName) {
                                                "$base ($counter).$extension"
                                            } else {
                                                "$base ($counter)"
                                            }
                                            val candCheck = destinationProvider.checkFileExists(
                                                name = candidateName,
                                                parentFolderId = parentDestId,
                                                checksum = null,
                                                size = item.fileSize
                                            )
                                            val candExists = candCheck is ProviderResult.Success && candCheck.data.exists
                                            if (!candExists) break
                                            counter++
                                        } while (counter <= 50)
                                        targetFileName = candidateName
                                        logEvent(job.id, "Duplicate resolved. Renaming to: $targetFileName", LogLevel.INFO)
                                    }
                                    ConflictStrategy.ASK_EVERY_TIME -> {
                                        shouldSkip = true
                                    }
                                }
                            }

                            if (shouldSkip) {
                                synchronized(this@TransferEngine) {
                                    skippedCount++
                                }
                                transferItemDao.updateItem(
                                    item.copy(
                                        status = ItemStatus.SKIPPED,
                                        verificationStatus = VerificationStatus.VERIFIED,
                                        completedAt = System.currentTimeMillis()
                                    )
                                )
                                return@withPermit
                            }

                            // 2. Stream & Chunk Transfer with transient retry
                            var attempts = 0
                            var lastError: Exception? = null
                            while (attempts < 3) {
                                if (pausedJobIds.contains(job.id)) return@withPermit
                                try {
                                    attempts++
                                    var bytesTransferred = 0L
                                    val downloadResult = sourceProvider.downloadFileStream(
                                        fileId = item.sourceFileId,
                                        onChunk = { chunk, bytesRead, total ->
                                            bytesTransferred = bytesRead
                                            val percent = if (total > 0) ((bytesRead * 100) / total).toInt() else 100
                                            transferItemDao.updateItem(
                                                item.copy(
                                                    bytesTransferred = bytesRead,
                                                    percentage = percent
                                                )
                                            )

                                            val currentTransferredSoFar = synchronized(this@TransferEngine) { totalTransferredBytes } + bytesRead
                                            val elapsedSec = maxOf(1L, (System.currentTimeMillis() - startTime) / 1000)
                                            val currentSpeed = currentTransferredSoFar / elapsedSec
                                            _engineSpeedBytesPerSec.value = currentSpeed
                                            val remainingBytes = maxOf(0L, totalBytes - currentTransferredSoFar)
                                            val etaSec = if (currentSpeed > 0) remainingBytes / currentSpeed else 0L

                                            val curCompleted = synchronized(this@TransferEngine) { completedCount }
                                            val curFailed = synchronized(this@TransferEngine) { failedCount }
                                            val curSkipped = synchronized(this@TransferEngine) { skippedCount }
                                            val curRemaining = maxOf(0, totalFiles - curCompleted - curFailed - curSkipped)

                                            realtimeWorker.pushProgressUpdate(
                                                jobId = job.id,
                                                filesCompleted = curCompleted,
                                                filesRemaining = curRemaining,
                                                totalFiles = totalFiles,
                                                failedFiles = curFailed,
                                                skippedFiles = curSkipped,
                                                bytesTransferred = currentTransferredSoFar,
                                                totalBytes = totalBytes,
                                                currentFileName = item.fileName,
                                                currentFileBytes = bytesRead,
                                                currentFileTotalBytes = total,
                                                transferSpeedBytesPerSec = currentSpeed,
                                                etaSeconds = etaSec
                                            )
                                        }
                                    )

                                    val filePayload = when (downloadResult) {
                                        is ProviderResult.Success -> downloadResult.data
                                        is ProviderResult.Error -> throw Exception("Download failed: ${downloadResult.message}")
                                    }

                                    // 3. Upload to Destination Provider
                                    val uploadResult = destinationProvider.uploadFile(
                                        name = targetFileName,
                                        mimeType = item.mimeType,
                                        size = item.fileSize,
                                        parentFolderId = parentDestId,
                                        data = filePayload,
                                        expectedChecksum = item.checksum,
                                        onProgress = { written, total ->
                                            // Live progress updates
                                        }
                                    )

                                    when (uploadResult) {
                                        is ProviderResult.Success -> {
                                            // 4. File Integrity & Verification Check
                                            transferItemDao.updateItem(
                                                item.copy(
                                                    status = ItemStatus.VERIFYING,
                                                    verificationStatus = VerificationStatus.TRANSFERRED
                                                )
                                            )

                                            val uploadedFile = uploadResult.data
                                            val sizeMatches = uploadedFile.size == item.fileSize || item.fileSize == 0L
                                            val checksumMatches = if (uploadedFile.checksum != null && item.checksum != null) {
                                                uploadedFile.checksum.equals(item.checksum, ignoreCase = true)
                                            } else {
                                                true
                                            }

                                            val isVerified = sizeMatches && checksumMatches
                                            val verifyStatus = if (isVerified) VerificationStatus.VERIFIED else VerificationStatus.FAILED_VERIFICATION

                                            transferItemDao.updateItem(
                                                item.copy(
                                                    status = if (isVerified) ItemStatus.COMPLETED else ItemStatus.FAILED,
                                                    verificationStatus = verifyStatus,
                                                    bytesTransferred = item.fileSize,
                                                    percentage = 100,
                                                    completedAt = System.currentTimeMillis(),
                                                    error = if (!isVerified) "Checksum/Size verification failed" else null
                                                )
                                            )

                                            synchronized(this@TransferEngine) {
                                                if (isVerified) {
                                                    completedCount++
                                                    totalTransferredBytes += item.fileSize
                                                } else {
                                                    failedCount++
                                                }
                                            }
                                            break // Success, exit retry loop
                                        }
                                        is ProviderResult.Error -> {
                                            throw Exception(uploadResult.message)
                                        }
                                    }
                                } catch (retryEx: Exception) {
                                    if (retryEx is CancellationException) throw retryEx
                                    lastError = retryEx
                                    if (attempts < 3) {
                                        logEvent(job.id, "Transient transfer failure on ${item.fileName}: ${retryEx.localizedMessage}. Retrying ($attempts/2)...", LogLevel.WARN)
                                        delay(250L * attempts)
                                    } else {
                                        throw retryEx
                                    }
                                }
                            }

                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            synchronized(this@TransferEngine) {
                                failedCount++
                            }
                            transferItemDao.updateItem(
                                item.copy(
                                    status = ItemStatus.FAILED,
                                    error = e.localizedMessage ?: "Unknown transfer error",
                                    completedAt = System.currentTimeMillis()
                                )
                            )
                            logEvent(job.id, "File failed (${item.fileName}): ${e.localizedMessage}", LogLevel.ERROR)
                        } finally {
                            // Update overall job stats
                            val elapsedSec = maxOf(1L, (System.currentTimeMillis() - startTime) / 1000)
                            val currentSpeed = totalTransferredBytes / elapsedSec
                            _engineSpeedBytesPerSec.value = currentSpeed
                            val remainingBytes = maxOf(0L, totalBytes - totalTransferredBytes)
                            val etaSec = if (currentSpeed > 0) remainingBytes / currentSpeed else 0L
                            val remainingFiles = maxOf(0, totalFiles - completedCount - failedCount - skippedCount)

                            realtimeWorker.pushProgressUpdate(
                                jobId = job.id,
                                filesCompleted = completedCount,
                                filesRemaining = remainingFiles,
                                totalFiles = totalFiles,
                                failedFiles = failedCount,
                                skippedFiles = skippedCount,
                                bytesTransferred = totalTransferredBytes,
                                totalBytes = totalBytes,
                                currentFileName = if (remainingFiles > 0) "Synchronizing pipeline..." else item.fileName,
                                currentFileBytes = item.fileSize,
                                currentFileTotalBytes = item.fileSize,
                                transferSpeedBytesPerSec = currentSpeed,
                                etaSeconds = etaSec
                            )

                            transferJobDao.updateJob(
                                job.copy(
                                    completedFiles = completedCount,
                                    failedFiles = failedCount,
                                    skippedFiles = skippedCount,
                                    transferredBytes = totalTransferredBytes,
                                    currentSpeedBytesPerSec = currentSpeed
                                )
                            )
                        }
                    }
                }
            }

            transferJobs.forEach { it.join() }

            if (pausedJobIds.contains(job.id)) {
                logEvent(job.id, "Transfer job paused. Preserved current progress.", LogLevel.INFO)
                return
            }

            // STEP 3: FINAL STATE
            val finalJob = transferJobDao.getJobByIdOnce(job.id) ?: job
            val finalStatus = when {
                failedCount > 0 && completedCount == 0 && skippedCount == 0 -> JobStatus.FAILED
                else -> JobStatus.COMPLETED
            }

            val endTime = System.currentTimeMillis()
            transferJobDao.updateJob(
                finalJob.copy(
                    status = finalStatus,
                    completedAt = endTime,
                    completedFiles = completedCount,
                    failedFiles = failedCount,
                    skippedFiles = skippedCount,
                    transferredBytes = totalTransferredBytes
                )
            )

            realtimeWorker.pushProgressUpdate(
                jobId = job.id,
                filesCompleted = completedCount,
                filesRemaining = 0,
                totalFiles = totalFiles,
                failedFiles = failedCount,
                skippedFiles = skippedCount,
                bytesTransferred = totalTransferredBytes,
                totalBytes = totalBytes,
                currentFileName = null,
                currentFileBytes = 0L,
                currentFileTotalBytes = 0L,
                transferSpeedBytesPerSec = 0L,
                etaSeconds = 0L
            )

            logEvent(
                job.id,
                "Transfer job finished with status $finalStatus: $completedCount completed, $skippedCount skipped, $failedCount failed.",
                if (finalStatus == JobStatus.COMPLETED) LogLevel.INFO else LogLevel.WARN
            )

            // Audit Log
            auditLogDao.insertLog(
                AuditLogEntity(
                    jobId = job.id,
                    userId = job.userId,
                    provider = "${job.sourceProvider} -> ${job.destinationProvider}",
                    operation = "CLOUD_TRANSFER",
                    status = finalStatus.name,
                    timestamp = endTime,
                    durationMs = endTime - startTime
                )
            )

        } catch (c: CancellationException) {
            logEvent(job.id, "Job execution cancelled", LogLevel.WARN)
        } catch (e: Exception) {
            realtimeWorker.pushProgressUpdate(
                jobId = job.id,
                filesCompleted = 0,
                filesRemaining = 0,
                totalFiles = 0,
                failedFiles = 1,
                skippedFiles = 0,
                bytesTransferred = 0L,
                totalBytes = 0L,
                currentFileName = null,
                currentFileBytes = 0L,
                currentFileTotalBytes = 0L,
                transferSpeedBytesPerSec = 0L,
                etaSeconds = 0L
            )
            transferJobDao.updateJob(
                job.copy(
                    status = JobStatus.FAILED,
                    errorMessage = e.localizedMessage,
                    completedAt = System.currentTimeMillis()
                )
            )
            logEvent(job.id, "Critical engine error: ${e.localizedMessage}", LogLevel.ERROR)
        } finally {
            activeExecutionJobs.remove(job.id)
            _engineSpeedBytesPerSec.value = 0L
        }
    }

    private suspend fun logEvent(jobId: String, message: String, level: LogLevel) {
        transferEventDao.insertEvent(
            TransferEventEntity(
                jobId = jobId,
                timestamp = System.currentTimeMillis(),
                message = message,
                level = level
            )
        )
    }
}
