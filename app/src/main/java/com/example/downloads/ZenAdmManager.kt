package com.example.downloads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ZenAdmManager {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val _tasks = MutableStateFlow<List<AdmDownloadTask>>(emptyList())
    val tasks: StateFlow<List<AdmDownloadTask>> = _tasks.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun startDownload(sub: AdmDownloadSubmission): AdmDownloadTask {
        val totalBytes = if (sub.totalBytes > 0) sub.totalBytes else 28_500_000L // Default estimate if unknown
        val parts = sub.partsCount.coerceIn(1, 16)

        val partSize = totalBytes / parts
        val segments = (0 until parts).map { idx ->
            val start = idx * partSize
            val end = if (idx == parts - 1) totalBytes else (start + partSize)
            AdmDownloadSegment(
                partIndex = idx,
                startByte = start,
                endByte = end,
                downloadedBytes = 0L,
                isDone = false
            )
        }

        val task = AdmDownloadTask(
            id = UUID.randomUUID().toString(),
            fileName = sub.fileName,
            url = sub.url,
            totalBytes = totalBytes,
            downloadedBytes = 0L,
            speedBytesPerSec = 0L,
            status = AdmDownloadStatus.DOWNLOADING,
            partsCount = parts,
            speedLimitBytesPerSec = sub.speedLimitBytesPerSec,
            unmeteredOnly = sub.unmeteredOnly,
            category = sub.category,
            startedAt = System.currentTimeMillis(),
            segments = segments
        )

        _tasks.update { listOf(task) + it }
        launchWorker(task.id)
        return task
    }

    private fun launchWorker(taskId: String) {
        activeJobs[taskId]?.cancel()
        val job = scope.launch {
            val baseSpeed = (2_500_000L..5_500_000L).random() // 2.5MB/s - 5.5MB/s
            while (isActive) {
                delay(400)
                var completed = false
                _tasks.update { list ->
                    list.map { item ->
                        if (item.id != taskId || item.status != AdmDownloadStatus.DOWNLOADING) {
                            item
                        } else {
                            val parts = item.partsCount
                            val speedBoost = (parts * 0.45f).coerceAtLeast(1.0f)
                            val currentSpeed = ((baseSpeed * speedBoost) * (0.85f + Math.random().toFloat() * 0.3f)).toLong()
                            val limitedSpeed = if (item.speedLimitBytesPerSec > 0) {
                                currentSpeed.coerceAtMost(item.speedLimitBytesPerSec)
                            } else currentSpeed

                            val chunk = (limitedSpeed * 0.4).toLong()
                            val nextDownloaded = (item.downloadedBytes + chunk).coerceAtMost(item.totalBytes)

                            // Update segments
                            val updatedSegments = item.segments.map { seg ->
                                val segTotal = seg.endByte - seg.startByte
                                val segChunk = (chunk / parts).coerceAtLeast(1L)
                                val segDownloaded = (seg.downloadedBytes + segChunk).coerceAtMost(segTotal)
                                seg.copy(
                                    downloadedBytes = segDownloaded,
                                    isDone = segDownloaded >= segTotal
                                )
                            }

                            if (nextDownloaded >= item.totalBytes) {
                                completed = true
                                item.copy(
                                    downloadedBytes = item.totalBytes,
                                    speedBytesPerSec = 0L,
                                    status = AdmDownloadStatus.COMPLETED,
                                    completedAt = System.currentTimeMillis(),
                                    segments = updatedSegments.map { it.copy(downloadedBytes = it.endByte - it.startByte, isDone = true) }
                                )
                            } else {
                                item.copy(
                                    downloadedBytes = nextDownloaded,
                                    speedBytesPerSec = limitedSpeed,
                                    segments = updatedSegments
                                )
                            }
                        }
                    }
                }

                if (completed) {
                    break
                }
            }
        }
        activeJobs[taskId] = job
    }

    fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        _tasks.update { list ->
            list.map {
                if (it.id == taskId && it.status == AdmDownloadStatus.DOWNLOADING) {
                    it.copy(status = AdmDownloadStatus.PAUSED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    fun resumeDownload(taskId: String) {
        _tasks.update { list ->
            list.map {
                if (it.id == taskId && it.status == AdmDownloadStatus.PAUSED) {
                    it.copy(status = AdmDownloadStatus.DOWNLOADING)
                } else it
            }
        }
        launchWorker(taskId)
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        _tasks.update { list -> list.filter { it.id != taskId } }
    }

    fun clearCompleted() {
        _tasks.update { list -> list.filter { it.status != AdmDownloadStatus.COMPLETED } }
    }
}
