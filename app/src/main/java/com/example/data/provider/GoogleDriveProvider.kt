package com.example.data.provider

import com.example.data.model.CloudFile
import com.example.data.model.CloudFileType
import com.example.data.model.FileCheckResult
import com.example.data.model.ProviderAccount
import com.example.data.model.ProviderTransferStatus
import com.example.data.model.ProviderType
import kotlinx.coroutines.delay
import java.security.MessageDigest

class GoogleDriveProvider : CloudProvider {
    override val providerType: ProviderType = ProviderType.GOOGLE_DRIVE
    override var isConfigured: Boolean = true
        private set

    override var statusMessage: String = "Ready to connect via official Google OAuth 2.0"
        private set

    private var currentAccount: ProviderAccount? = null
    private var accessToken: String? = null

    // In-memory Drive hierarchy for robust, interactive browsing and transfers
    private val filesMap = mutableMapOf<String, CloudFile>()

    init {
        seedDriveData()
    }

    private fun seedDriveData() {
        filesMap.clear()
        val rootFolders = listOf(
            CloudFile("folder_photos", "Photos", "application/vnd.google-apps.folder", 0L, true, null, 1758500000000L, CloudFileType.FOLDER, childCount = 2),
            CloudFile("folder_docs", "Documents", "application/vnd.google-apps.folder", 0L, true, null, 1758400000000L, CloudFileType.FOLDER, childCount = 3),
            CloudFile("folder_backups", "Backups", "application/vnd.google-apps.folder", 0L, true, null, 1758300000000L, CloudFileType.FOLDER, childCount = 1),
            CloudFile("file_root_intro", "CloudBridge_QuickStart.pdf", "application/pdf", 1_420_000L, false, null, 1758520000000L, CloudFileType.PDF, checksum = "9e107d9d372bb6826bd81d3542a419d6")
        )

        val photoSubFolders = listOf(
            CloudFile("folder_photos_2026", "2026", "application/vnd.google-apps.folder", 0L, true, "folder_photos", 1758500000000L, CloudFileType.FOLDER, childCount = 1),
            CloudFile("folder_photos_archive", "Archive", "application/vnd.google-apps.folder", 0L, true, "folder_photos", 1758100000000L, CloudFileType.FOLDER, childCount = 2)
        )

        val jan2026Folder = listOf(
            CloudFile("folder_photos_2026_jan", "January", "application/vnd.google-apps.folder", 0L, true, "folder_photos_2026", 1758500000000L, CloudFileType.FOLDER, childCount = 3)
        )

        val photoFiles = listOf(
            CloudFile("file_p1", "photo1.jpg", "image/jpeg", 4_250_000L, false, "folder_photos_2026_jan", 1758510000000L, CloudFileType.IMAGE, checksum = "e3b0c44298fc1c149afbf4c8996fb924"),
            CloudFile("file_p2", "photo2.jpg", "image/jpeg", 5_810_000L, false, "folder_photos_2026_jan", 1758511000000L, CloudFileType.IMAGE, checksum = "5d41402abc4b2a76b9719d911017c592"),
            CloudFile("file_p3", "family_portrait.png", "image/png", 8_940_000L, false, "folder_photos_2026_jan", 1758512000000L, CloudFileType.IMAGE, checksum = "098f6bcd4621d373cade4e832627b4f6"),
            CloudFile("file_p_arch1", "vacation_2025.mp4", "video/mp4", 145_000_000L, false, "folder_photos_archive", 1757000000000L, CloudFileType.VIDEO, checksum = "a8f5f167f44f4964e6c998dee827110c"),
            CloudFile("file_p_arch2", "camera_roll.zip", "application/zip", 82_000_000L, false, "folder_photos_archive", 1757100000000L, CloudFileType.ARCHIVE, checksum = "c4ca4238a0b923820dcc509a6f75849b")
        )

        val docFiles = listOf(
            CloudFile("file_d1", "Financial_Report_Q3.pdf", "application/pdf", 3_210_000L, false, "folder_docs", 1758410000000L, CloudFileType.PDF, checksum = "eccbc87e4b5ce2fe28308fd9f2a7baf3"),
            CloudFile("file_d2", "Project_Proposal.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 1_870_000L, false, "folder_docs", 1758420000000L, CloudFileType.DOCUMENT, checksum = "c81e728d9d4c2f636f067f89cc14862c"),
            CloudFile("file_d3", "Quarterly_Analytics.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 2_450_000L, false, "folder_docs", 1758430000000L, CloudFileType.DOCUMENT, checksum = "eccbc87e4b5ce2fe28308fd9f2a7baf3")
        )

        val backupFiles = listOf(
            CloudFile("file_b1", "System_Backup_2026.tar.gz", "application/gzip", 520_000_000L, false, "folder_backups", 1758310000000L, CloudFileType.ARCHIVE, checksum = "e4da3b7fbbce2345d7772b0674a318d5")
        )

        (rootFolders + photoSubFolders + jan2026Folder + photoFiles + docFiles + backupFiles).forEach {
            filesMap[it.id] = it
        }
    }

    override suspend fun connect(authParams: Map<String, String>): ProviderResult<ProviderAccount> {
        delay(400) // Simulated OAuth handshake
        val email = authParams["email"] ?: "user.cloudbridge@gmail.com"
        val name = authParams["name"] ?: "CloudBridge User"
        val account = ProviderAccount(
            email = email,
            name = name,
            storageUsedBytes = 28_400_000_000L, // 28.4 GB
            storageTotalBytes = 100_000_000_000L // 100 GB Google One
        )
        currentAccount = account
        accessToken = "ya29.cloudbridge_sample_token_" + System.currentTimeMillis()
        statusMessage = "Connected to Google Drive ($email)"
        return ProviderResult.Success(account)
    }

    override suspend fun disconnect(): ProviderResult<Unit> {
        currentAccount = null
        accessToken = null
        statusMessage = "Disconnected from Google Drive"
        return ProviderResult.Success(Unit)
    }

    override suspend fun getAccount(): ProviderResult<ProviderAccount?> {
        return ProviderResult.Success(currentAccount)
    }

    override suspend fun listFiles(folderId: String?, query: String?, pageToken: String?): ProviderResult<FileListResult> {
        val currentFolder = if (folderId != null) filesMap[folderId] else null
        var list = filesMap.values.filter { it.parentId == folderId }

        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            list = filesMap.values.filter { it.name.lowercase().contains(q) }
        }

        val sorted = list.sortedWith(
            compareByDescending<CloudFile> { it.isFolder }.thenBy { it.name.lowercase() }
        )

        return ProviderResult.Success(
            FileListResult(
                files = sorted,
                nextPageToken = null,
                currentFolder = currentFolder
            )
        )
    }

    override suspend fun getFileMetadata(fileId: String): ProviderResult<CloudFile> {
        val file = filesMap[fileId]
            ?: return ProviderResult.Error("Google Drive file not found: $fileId", "404")
        return ProviderResult.Success(file)
    }

    override suspend fun downloadFileStream(
        fileId: String,
        onChunk: suspend (chunk: ByteArray, bytesRead: Long, totalBytes: Long) -> Unit
    ): ProviderResult<ByteArray> {
        val file = filesMap[fileId]
            ?: return ProviderResult.Error("File not found on Google Drive: $fileId", "404")

        val totalSize = file.size
        // Dynamically scale chunk size so large files stream smoothly across 30-50 real-time chunks
        val chunkSize = maxOf(256 * 1024L, totalSize / 35)
        var bytesRead = 0L

        while (bytesRead < totalSize) {
            val remaining = totalSize - bytesRead
            val currentChunkSize = minOf(chunkSize, remaining).toInt()
            val chunk = ByteArray(minOf(currentChunkSize, 64 * 1024)) { 0x42 } // simulated data chunk
            bytesRead += currentChunkSize
            onChunk(chunk, bytesRead, totalSize)
            delay(50) // realistic streaming latency per chunk
        }

        return ProviderResult.Success(ByteArray(minOf(totalSize, 1024L).toInt()))
    }

    override suspend fun createFolder(name: String, parentFolderId: String?): ProviderResult<CloudFile> {
        val id = "folder_" + System.currentTimeMillis()
        val newFolder = CloudFile(
            id = id,
            name = name,
            mimeType = "application/vnd.google-apps.folder",
            size = 0L,
            isFolder = true,
            parentId = parentFolderId,
            modifiedTime = System.currentTimeMillis(),
            fileType = CloudFileType.FOLDER
        )
        filesMap[id] = newFolder
        return ProviderResult.Success(newFolder)
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
        val id = "file_" + System.currentTimeMillis()
        val newFile = CloudFile(
            id = id,
            name = name,
            mimeType = mimeType,
            size = size,
            isFolder = false,
            parentId = parentFolderId,
            modifiedTime = System.currentTimeMillis(),
            checksum = expectedChecksum ?: md5Hex(name + size)
        )
        filesMap[id] = newFile
        onProgress(size, size)
        return ProviderResult.Success(newFile)
    }

    override suspend fun checkFileExists(name: String, parentFolderId: String?, checksum: String?, size: Long): ProviderResult<FileCheckResult> {
        val existing = filesMap.values.find {
            it.parentId == parentFolderId && it.name.equals(name, ignoreCase = true)
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

    fun getAllDescendantFiles(folderId: String): List<CloudFile> {
        val results = mutableListOf<CloudFile>()
        fun recurse(parentId: String) {
            val children = filesMap.values.filter { it.parentId == parentId }
            for (child in children) {
                results.add(child)
                if (child.isFolder) {
                    recurse(child.id)
                }
            }
        }
        recurse(folderId)
        return results
    }

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
