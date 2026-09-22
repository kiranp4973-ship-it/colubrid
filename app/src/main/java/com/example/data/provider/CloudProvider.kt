package com.example.data.provider

import com.example.data.model.CloudFile
import com.example.data.model.ConnectionStatus
import com.example.data.model.FileCheckResult
import com.example.data.model.ProviderAccount
import com.example.data.model.ProviderTransferStatus
import com.example.data.model.ProviderType

sealed class ProviderResult<out T> {
    data class Success<out T>(val data: T) : ProviderResult<T>()
    data class Error(val message: String, val code: String? = null, val throwable: Throwable? = null) : ProviderResult<Nothing>()
}

data class FileListResult(
    val files: List<CloudFile>,
    val nextPageToken: String? = null,
    val currentFolder: CloudFile? = null
)

interface CloudProvider {
    val providerType: ProviderType
    val isConfigured: Boolean
    val statusMessage: String

    suspend fun connect(authParams: Map<String, String> = emptyMap()): ProviderResult<ProviderAccount>
    suspend fun disconnect(): ProviderResult<Unit>
    suspend fun getAccount(): ProviderResult<ProviderAccount?>
    suspend fun listFiles(folderId: String? = null, query: String? = null, pageToken: String? = null): ProviderResult<FileListResult>
    suspend fun getFileMetadata(fileId: String): ProviderResult<CloudFile>
    suspend fun downloadFileStream(
        fileId: String,
        onChunk: suspend (chunk: ByteArray, bytesRead: Long, totalBytes: Long) -> Unit
    ): ProviderResult<ByteArray>
    suspend fun createFolder(name: String, parentFolderId: String? = null): ProviderResult<CloudFile>
    suspend fun uploadFile(
        name: String,
        mimeType: String,
        size: Long,
        parentFolderId: String? = null,
        data: ByteArray,
        expectedChecksum: String? = null,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): ProviderResult<CloudFile>
    suspend fun checkFileExists(name: String, parentFolderId: String? = null, checksum: String? = null, size: Long = 0L): ProviderResult<FileCheckResult>
    suspend fun getTransferStatus(transferId: String): ProviderResult<ProviderTransferStatus>
}
