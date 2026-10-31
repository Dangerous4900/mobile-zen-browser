package com.example.downloads

import java.util.UUID

enum class AdmCategory(val label: String) {
    ALL("All"),
    VIDEO("Video"),
    AUDIO("Audio"),
    DOCUMENT("Document"),
    ARCHIVE("Archive"),
    SOFTWARE("Software")
}

enum class AdmDownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

data class AdmDownloadSegment(
    val partIndex: Int,
    val startByte: Long,
    val endByte: Long,
    var downloadedBytes: Long,
    var isDone: Boolean = false
) {
    val progress: Float
        get() {
            val total = endByte - startByte
            return if (total > 0) (downloadedBytes.toFloat() / total).coerceIn(0f, 1f) else 1f
        }
}

data class AdmDownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: AdmDownloadStatus = AdmDownloadStatus.DOWNLOADING,
    val partsCount: Int = 8,
    val speedLimitBytesPerSec: Long = 0L,
    val unmeteredOnly: Boolean = false,
    val category: AdmCategory = AdmCategory.ALL,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val segments: List<AdmDownloadSegment> = emptyList()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val formattedSpeed: String
        get() = when {
            speedBytesPerSec > 1_000_000 -> String.format(java.util.Locale.US, "%.1f MB/s", speedBytesPerSec / 1_000_000.0)
            speedBytesPerSec > 1_000 -> String.format(java.util.Locale.US, "%.0f KB/s", speedBytesPerSec / 1_000.0)
            else -> "$speedBytesPerSec B/s"
        }

    val formattedDownloaded: String
        get() = formatBytes(downloadedBytes)

    val formattedTotal: String
        get() = if (totalBytes > 0) formatBytes(totalBytes) else "Unknown"
}

data class AdmDownloadSubmission(
    val url: String,
    val fileName: String,
    val totalBytes: Long = 0L,
    val partsCount: Int = 8,
    val speedLimitBytesPerSec: Long = 0L,
    val unmeteredOnly: Boolean = false,
    val category: AdmCategory = AdmCategory.ALL
)

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
