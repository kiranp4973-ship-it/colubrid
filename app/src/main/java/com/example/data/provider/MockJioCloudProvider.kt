package com.example.data.provider

import com.example.data.model.CloudFile
import com.example.data.model.CloudFileType
import com.example.data.model.FileCheckResult
import com.example.data.model.ProviderAccount
import com.example.data.model.ProviderTransferStatus
import com.example.data.model.ProviderType
import kotlinx.coroutines.delay
import java.security.MessageDigest

/**
 * MockJioCloudProvider for Development Mode.
 * Strictly labeled: "Development mode — simulated JioCloud destination."
 * Allows complete end-to-end verification of the Transfer Engine, folder recursion,
 * chunked streaming, checksum integrity verification, and duplicate handling.
 */
class MockJioCloudProvider : CloudProvider {
    override val providerType: ProviderType = ProviderType.JIO_CLOUD
    override val isConfigured: Boolean = true
    override val statusMessage: String = "Development mode — simulated JioCloud destination"

    private val destinationFiles = mutableMapOf<String, CloudFile>()
    private var isConnected = true

    init {
        seedJioFiles()
    }

    private fun seedJioFiles() {
        destinationFiles.clear()
        val rootTransferFolder = CloudFile(
            id = "jio_root_transfers",
            name = "CloudBridge Transfers",
            mimeType = "application/vnd.google-apps.folder",
            size = 0L,
            isFolder = true,
            parentId = null,
            fileType = CloudFileType.FOLDER,
            childCount = 0
        )
        val docsFolder = CloudFile(
            id = "jio_f_docs",
            name = "Jio Documents",
            mimeType = "application/vnd.google-apps.folder",
            size = 0L,
            isFolder = true,
            parentId = null,
            fileType = CloudFileType.FOLDER,
            childCount = 2
        )
        val mediaFolder = CloudFile(
            id = "jio_f_media",
            name = "Media Vault",
            mimeType = "application/vnd.google-apps.folder",
            size = 0L,
            isFolder = true,
            parentId = null,
            fileType = CloudFileType.FOLDER,
            childCount = 2
        )
        val rootFile1 = CloudFile(
            id = "jio_f_tax",
            name = "Annual_Tax_Statement_2026.pdf",
            mimeType = "application/pdf",
            size = 2_150_000L,
            isFolder = false,
            parentId = null,
            fileType = CloudFileType.PDF,
            checksum = "a1b2c3d4e5f67890123456789abcdef0"
        )
        val rootFile2 = CloudFile(
            id = "jio_f_welcome",
            name = "JioCloud_Getting_Started.pdf",
            mimeType = "application/pdf",
            size = 1_180_000L,
            isFolder = false,
            parentId = null,
            fileType = CloudFileType.PDF,
            checksum = "b2c3d4e5f67890123456789abcdef01a"
        )

        val doc1 = CloudFile(
            id = "jio_doc_1",
            name = "Consulting_Agreement.docx",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            size = 1_420_000L,
            isFolder = false,
            parentId = "jio_f_docs",
            fileType = CloudFileType.DOCUMENT,
            checksum = "c3d4e5f67890123456789abcdef01a2b"
        )
        val doc2 = CloudFile(
            id = "jio_doc_2",
            name = "Project_Budget_2026.xlsx",
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            size = 3_280_000L,
            isFolder = false,
            parentId = "jio_f_docs",
            fileType = CloudFileType.DOCUMENT,
            checksum = "d4e5f67890123456789abcdef01a2b3c"
        )

        val media1 = CloudFile(
            id = "jio_med_1",
            name = "Himalayan_Summit.jpg",
            mimeType = "image/jpeg",
            size = 4_820_000L,
            isFolder = false,
            parentId = "jio_f_media",
            fileType = CloudFileType.IMAGE,
            checksum = "e5f67890123456789abcdef01a2b3c4d"
        )
        val media2 = CloudFile(
            id = "jio_med_2",
            name = "Keynote_Presentation.mp4",
            mimeType = "video/mp4",
            size = 112_000_000L,
            isFolder = false,
            parentId = "jio_f_media",
            fileType = CloudFileType.VIDEO,
            checksum = "f67890123456789abcdef01a2b3c4d5e"
        )

        listOf(rootTransferFolder, docsFolder, mediaFolder, rootFile1, rootFile2, doc1, doc2, media1, media2).forEach {
            destinationFiles[it.id] = it
        }
    }

    override suspend fun connect(authParams: Map<String, String>): ProviderResult<ProviderAccount> {
        delay(200)
        isConnected = true
        return ProviderResult.Success(
            ProviderAccount(
                email = "developer.simulated@jiocloud.mock",
                name = "JioCloud (Simulated Dev Destination)",
                storageUsedBytes = 12_800_000_000L,
                storageTotalBytes = 100_000_000_000L
            )
        )
    }

    override suspend fun disconnect(): ProviderResult<Unit> {
        isConnected = false
        return ProviderResult.Success(Unit)
    }

    override suspend fun getAccount(): ProviderResult<ProviderAccount?> {
        return if (isConnected) {
            ProviderResult.Success(
                ProviderAccount(
                    email = "developer.simulated@jiocloud.mock",
                    name = "JioCloud (Simulated Dev Destination)",
                    storageUsedBytes = destinationFiles.values.filter { !it.isFolder }.sumOf { it.size },
                    storageTotalBytes = 100_000_000_000L
                )
            )
        } else {
            ProviderResult.Success(null)
        }
    }

    override suspend fun listFiles(folderId: String?, query: String?, pageToken: String?): ProviderResult<FileListResult> {
        val currentFolder = if (folderId != null) destinationFiles[folderId] else null
        var items = destinationFiles.values.filter { it.parentId == folderId }
        if (!query.isNullOrBlank()) {
            items = destinationFiles.values.filter { it.name.contains(query, ignoreCase = true) }
        }
        return ProviderResult.Success(
            FileListResult(
                files = items.sortedWith(compareByDescending<CloudFile> { it.isFolder }.thenBy { it.name.lowercase() }),
                nextPageToken = null,
                currentFolder = currentFolder
            )
        )
    }

    override suspend fun getFileMetadata(fileId: String): ProviderResult<CloudFile> {
        val file = destinationFiles[fileId]
            ?: return ProviderResult.Error("File not found on simulated JioCloud: $fileId", "404")
        return ProviderResult.Success(file)
    }

    override suspend fun downloadFileStream(
        fileId: String,
        onChunk: suspend (chunk: ByteArray, bytesRead: Long, totalBytes: Long) -> Unit
    ): ProviderResult<ByteArray> {
        val file = destinationFiles[fileId]
            ?: return ProviderResult.Error("File not found on simulated JioCloud: $fileId", "404")
        val simulatedData = ByteArray(minOf(file.size.toInt(), 1024 * 64)) { (it % 256).toByte() }
        onChunk(simulatedData, file.size, file.size)
        return ProviderResult.Success(simulatedData)
    }

    override suspend fun createFolder(name: String, parentFolderId: String?): ProviderResult<CloudFile> {
        // If folder with same name in same parent already exists, return it
        val existing = destinationFiles.values.find { it.isFolder && it.name == name && it.parentId == parentFolderId }
        if (existing != null) {
            return ProviderResult.Success(existing)
        }

        val id = "jio_f_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val folder = CloudFile(
            id = id,
            name = name,
            mimeType = "application/vnd.google-apps.folder",
            size = 0L,
            isFolder = true,
            parentId = parentFolderId,
            modifiedTime = System.currentTimeMillis(),
            fileType = CloudFileType.FOLDER
        )
        destinationFiles[id] = folder
        return ProviderResult.Success(folder)
    }

    override suspend fun uploadFile(
        name: String,
        mimeType: String,
        size: Long,
        parentFolderId: String?,
        data: ByteArray,
        expectedChecksum: String?,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): ProviderResult<CloudFile> {
        val id = "jio_file_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val simulatedChecksum = expectedChecksum ?: computeChecksum(name, size)

        // Simulate chunk upload progress
        val totalSize = maxOf(size, 1L)
        val step = maxOf(1L, totalSize / 4)
        var written = 0L
        while (written < totalSize) {
            written = minOf(totalSize, written + step)
            onProgress(written, totalSize)
            delay(20) // simulate upload latency
        }

        val newFile = CloudFile(
            id = id,
            name = name,
            mimeType = mimeType,
            size = size,
            isFolder = false,
            parentId = parentFolderId,
            modifiedTime = System.currentTimeMillis(),
            checksum = simulatedChecksum
        )
        destinationFiles[id] = newFile
        return ProviderResult.Success(newFile)
    }

    override suspend fun checkFileExists(name: String, parentFolderId: String?, checksum: String?, size: Long): ProviderResult<FileCheckResult> {
        val existing = destinationFiles.values.find {
            !it.isFolder && it.name.equals(name, ignoreCase = true) && it.parentId == parentFolderId
        }
        return if (existing != null) {
            val checksumMatch = (checksum != null && existing.checksum != null && checksum.equals(existing.checksum, ignoreCase = true))
                    || (existing.size == size && size > 0)
            ProviderResult.Success(
                FileCheckResult(
                    exists = true,
                    existingFileId = existing.id,
                    existingSize = existing.size,
                    checksumMatch = checksumMatch
                )
            )
        } else {
            ProviderResult.Success(FileCheckResult(exists = false))
        }
    }

    override suspend fun getTransferStatus(transferId: String): ProviderResult<ProviderTransferStatus> {
        return ProviderResult.Success(
            ProviderTransferStatus(
                transferId = transferId,
                bytesTransferred = 0L,
                totalBytes = 0L,
                isFinished = true
            )
        )
    }

    private fun computeChecksum(name: String, size: Long): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest("$name-$size-mock-jio".toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
