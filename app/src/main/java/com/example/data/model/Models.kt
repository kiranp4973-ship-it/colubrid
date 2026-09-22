package com.example.data.model

enum class ProviderType(val displayName: String) {
    GOOGLE_DRIVE("Google Drive"),
    JIO_CLOUD("JioCloud"),
    ONEDRIVE("OneDrive"),
    DROPBOX("Dropbox")
}

enum class ConnectionStatus {
    CONNECTED,
    NOT_CONNECTED,
    NOT_CONFIGURED,
    ERROR,
    COMING_SOON
}

enum class JobStatus {
    QUEUED,
    PREPARING,
    TRANSFERRING,
    VERIFYING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED,
    SKIPPED
}

enum class ItemStatus {
    QUEUED,
    PREPARING,
    TRANSFERRING,
    VERIFYING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED,
    SKIPPED
}

enum class VerificationStatus {
    PENDING,
    TRANSFERRED,
    VERIFIED,
    FAILED_VERIFICATION
}

enum class ConflictStrategy(val title: String, val description: String) {
    SKIP_DUPLICATE("Skip duplicate", "Skip copying if file with matching name and size exists in destination"),
    REPLACE("Replace", "Overwrite destination file with the source file"),
    KEEP_BOTH("Keep both", "Save source file with a sequential number suffix (e.g. file (1).png)"),
    ASK_EVERY_TIME("Ask every time", "Prompt before transferring conflicting files")
}

enum class UserRole {
    USER,
    ADMIN
}

enum class LogLevel {
    INFO,
    WARN,
    ERROR
}

enum class CloudFileType {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    PDF,
    DOCUMENT,
    ARCHIVE,
    OTHER
}

data class CloudFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val isFolder: Boolean,
    val parentId: String? = null,
    val modifiedTime: Long = System.currentTimeMillis(),
    val fileType: CloudFileType = mapMimeToType(mimeType, isFolder),
    val checksum: String? = null,
    val childCount: Int = 0
)

data class ProviderAccount(
    val email: String,
    val name: String,
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val avatarUrl: String? = null
)

data class FileCheckResult(
    val exists: Boolean,
    val existingFileId: String? = null,
    val existingSize: Long = 0L,
    val checksumMatch: Boolean = false
)

data class ProviderTransferStatus(
    val transferId: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val isFinished: Boolean,
    val error: String? = null
)

fun mapMimeToType(mime: String, isFolder: Boolean): CloudFileType {
    if (isFolder || mime == "application/vnd.google-apps.folder") return CloudFileType.FOLDER
    return when {
        mime.startsWith("image/") -> CloudFileType.IMAGE
        mime.startsWith("video/") -> CloudFileType.VIDEO
        mime.startsWith("audio/") -> CloudFileType.AUDIO
        mime == "application/pdf" -> CloudFileType.PDF
        mime.contains("document") || mime.contains("text") || mime.contains("word") || mime.contains("spreadsheet") || mime.contains("presentation") -> CloudFileType.DOCUMENT
        mime.contains("zip") || mime.contains("tar") || mime.contains("rar") || mime.contains("7z") || mime.contains("compressed") -> CloudFileType.ARCHIVE
        else -> CloudFileType.OTHER
    }
}
