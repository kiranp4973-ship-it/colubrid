package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.TransferEngine
import com.example.data.local.CloudBridgeDatabase
import com.example.data.model.CloudFileType
import com.example.data.model.ConflictStrategy
import com.example.data.model.ConnectionStatus
import com.example.data.model.ItemStatus
import com.example.data.model.JobStatus
import com.example.data.model.VerificationStatus
import com.example.data.provider.GoogleDriveProvider
import com.example.data.provider.JioCloudProvider
import com.example.data.provider.MockJioCloudProvider
import com.example.data.provider.ProviderResult
import com.example.data.realtime.RealtimeConnectionStatus
import com.example.data.realtime.RealtimeTransferWorker
import com.example.data.repository.CloudBridgeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudBridgeHealthCheckTest {

    private lateinit var context: Context
    private lateinit var database: CloudBridgeDatabase
    private lateinit var googleDriveProvider: GoogleDriveProvider
    private lateinit var mockJioCloudProvider: MockJioCloudProvider
    private lateinit var jioCloudProvider: JioCloudProvider
    private lateinit var realtimeWorker: RealtimeTransferWorker
    private lateinit var transferEngine: TransferEngine
    private lateinit var repository: CloudBridgeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = CloudBridgeDatabase.getInMemoryDatabase(context)
        repository = CloudBridgeRepository(database)
        googleDriveProvider = repository.googleDriveProvider
        mockJioCloudProvider = repository.mockJioCloudProvider
        jioCloudProvider = repository.jioCloudProvider
        transferEngine = repository.transferEngine
        realtimeWorker = repository.transferEngine.realtimeWorker
    }

    @After
    fun tearDown() {
        repository.shutdown()
        database.close()
    }

    @Test
    fun test1_GoogleDriveFileListingAndNavigation() = runTest {
        // Root directory list
        val rootResult = googleDriveProvider.listFiles(folderId = null)
        assertTrue(rootResult is ProviderResult.Success)
        val files = (rootResult as ProviderResult.Success).data.files
        assertTrue("Drive should have seed files/folders", files.isNotEmpty())

        val docsFolder = files.find { it.name == "Documents" && it.isFolder }
        assertNotNull("Documents folder should exist", docsFolder)

        // Nested folder listing
        val nestedResult = googleDriveProvider.listFiles(folderId = docsFolder!!.id)
        assertTrue(nestedResult is ProviderResult.Success)
        val nestedFiles = (nestedResult as ProviderResult.Success).data.files
        assertTrue("Documents folder should contain child files", nestedFiles.isNotEmpty())

        // Download streaming
        val firstFile = nestedFiles.first { !it.isFolder }
        var chunksReceived = 0
        val downloadResult = googleDriveProvider.downloadFileStream(firstFile.id) { _, _, _ ->
            chunksReceived++
        }
        assertTrue(downloadResult is ProviderResult.Success)
        assertTrue("Should have streamed at least one chunk", chunksReceived > 0)
    }

    @Test
    fun test2_JioCloudProviderDevModeAndProductionCheck() = runTest {
        // Mock provider is configured for development
        assertTrue(mockJioCloudProvider.isConfigured)
        val mockAccount = mockJioCloudProvider.connect()
        assertTrue(mockAccount is ProviderResult.Success)

        // Official provider requires configuration
        assertFalse(jioCloudProvider.isConfigured)
        val prodConnectResult = jioCloudProvider.connect()
        assertTrue(prodConnectResult is ProviderResult.Error)
        assertEquals("JIOCLOUD_NOT_CONFIGURED", (prodConnectResult as ProviderResult.Error).code)
    }

    @Test
    fun test3_TransferEngineEndToEndWithVerification() = runTest {
        // Select a file from Google Drive
        val rootResult = googleDriveProvider.listFiles(folderId = null) as ProviderResult.Success
        val selectedFile = rootResult.data.files.first { !it.isFolder }

        val jobId = repository.createAndStartTransferJob(
            selectedFiles = listOf(selectedFile),
            destinationPath = "/CloudBridge Test/",
            conflictStrategy = ConflictStrategy.SKIP_DUPLICATE
        )

        transferEngine.getActiveExecutionJob(jobId)?.join()

        val job = database.transferJobDao().getJobByIdOnce(jobId)
        assertNotNull(job)
        assertEquals(JobStatus.COMPLETED, job!!.status)
        assertEquals(1, job.completedFiles)
        assertEquals(0, job.failedFiles)

        val items = database.transferItemDao().getItemsForJobOnce(jobId)
        assertEquals(1, items.size)
        assertEquals(ItemStatus.COMPLETED, items.first().status)
        assertEquals(VerificationStatus.VERIFIED, items.first().verificationStatus)
    }

    @Test
    fun test4_DuplicateHandlingConflictStrategies() = runTest {
        val rootResult = googleDriveProvider.listFiles(folderId = null) as ProviderResult.Success
        val sourceFile = rootResult.data.files.first { !it.isFolder }

        // Pre-upload the file to destination to create a duplicate situation
        mockJioCloudProvider.uploadFile(
            name = sourceFile.name,
            mimeType = sourceFile.mimeType,
            size = sourceFile.size,
            parentFolderId = null,
            data = ByteArray(16),
            expectedChecksum = sourceFile.checksum
        ) { _, _ -> }

        // Test SKIP_DUPLICATE strategy
        val skipJobId = repository.createAndStartTransferJob(
            selectedFiles = listOf(sourceFile),
            destinationPath = "/",
            conflictStrategy = ConflictStrategy.SKIP_DUPLICATE
        )

        transferEngine.getActiveExecutionJob(skipJobId)?.join()

        val skipJob = database.transferJobDao().getJobByIdOnce(skipJobId)
        assertNotNull(skipJob)
        assertEquals(JobStatus.COMPLETED, skipJob!!.status)
        assertEquals(1, skipJob.skippedFiles)

        // Test KEEP_BOTH strategy: should rename duplicate to (1)
        val keepBothJobId = repository.createAndStartTransferJob(
            selectedFiles = listOf(sourceFile),
            destinationPath = "/",
            conflictStrategy = ConflictStrategy.KEEP_BOTH
        )

        transferEngine.getActiveExecutionJob(keepBothJobId)?.join()

        val keepBothJob = database.transferJobDao().getJobByIdOnce(keepBothJobId)
        assertNotNull(keepBothJob)
        assertEquals(JobStatus.COMPLETED, keepBothJob!!.status)
        assertEquals(1, keepBothJob.completedFiles)
    }

    @Test
    fun test5_RealtimeTransferWorkerDiagnosticsAndFallback() = runTest {
        // Initial state is CONNECTED or CONNECTING
        assertTrue(
            realtimeWorker.connectionStatus.value in listOf(
                RealtimeConnectionStatus.CONNECTING,
                RealtimeConnectionStatus.CONNECTED
            )
        )

        // Force network error
        realtimeWorker.toggleSimulateError(true)
        assertEquals(RealtimeConnectionStatus.ERROR, realtimeWorker.connectionStatus.value)

        // Reset to auto-reconnect
        realtimeWorker.toggleSimulateError(false)
        assertTrue(
            realtimeWorker.connectionStatus.value in listOf(
                RealtimeConnectionStatus.CONNECTING,
                RealtimeConnectionStatus.CONNECTED
            )
        )
    }
}
