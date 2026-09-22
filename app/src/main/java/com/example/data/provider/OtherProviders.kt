package com.example.data.provider

import com.example.data.model.CloudFile
import com.example.data.model.FileCheckResult
import com.example.data.model.ProviderAccount
import com.example.data.model.ProviderTransferStatus
import com.example.data.model.ProviderType

class OneDriveProvider : CloudProvider {
    override val providerType: ProviderType = ProviderType.ONEDRIVE
    override val isConfigured: Boolean = false
    override val statusMessage: String = "Coming soon in future release"

    override suspend fun connect(authParams: Map<String, String>) = ProviderResult.Error("OneDrive integration coming soon", "COMING_SOON")
    override suspend fun disconnect() = ProviderResult.Success(Unit)
    override suspend fun getAccount() = ProviderResult.Success(null)
    override suspend fun listFiles(folderId: String?, query: String?, pageToken: String?) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun getFileMetadata(fileId: String) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun downloadFileStream(fileId: String, onChunk: suspend (ByteArray, Long, Long) -> Unit) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun createFolder(name: String, parentFolderId: String?) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun uploadFile(name: String, mimeType: String, size: Long, parentFolderId: String?, data: ByteArray, expectedChecksum: String?, onProgress: (Long, Long) -> Unit) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun checkFileExists(name: String, parentFolderId: String?, checksum: String?, size: Long) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun getTransferStatus(transferId: String) = ProviderResult.Error("Coming soon", "COMING_SOON")
}

class DropboxProvider : CloudProvider {
    override val providerType: ProviderType = ProviderType.DROPBOX
    override val isConfigured: Boolean = false
    override val statusMessage: String = "Coming soon in future release"

    override suspend fun connect(authParams: Map<String, String>) = ProviderResult.Error("Dropbox integration coming soon", "COMING_SOON")
    override suspend fun disconnect() = ProviderResult.Success(Unit)
    override suspend fun getAccount() = ProviderResult.Success(null)
    override suspend fun listFiles(folderId: String?, query: String?, pageToken: String?) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun getFileMetadata(fileId: String) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun downloadFileStream(fileId: String, onChunk: suspend (ByteArray, Long, Long) -> Unit) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun createFolder(name: String, parentFolderId: String?) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun uploadFile(name: String, mimeType: String, size: Long, parentFolderId: String?, data: ByteArray, expectedChecksum: String?, onProgress: (Long, Long) -> Unit) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun checkFileExists(name: String, parentFolderId: String?, checksum: String?, size: Long) = ProviderResult.Error("Coming soon", "COMING_SOON")
    override suspend fun getTransferStatus(transferId: String) = ProviderResult.Error("Coming soon", "COMING_SOON")
}
