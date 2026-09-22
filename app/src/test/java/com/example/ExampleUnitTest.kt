package com.example

import com.example.data.realtime.RealtimeConnectionStatus
import com.example.data.realtime.RealtimeProtocol
import com.example.data.realtime.RealtimeTransferProgress
import com.example.data.realtime.RealtimeTransferWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testRealtimeTransferProgressCalculations() {
    val progress = RealtimeTransferProgress(
      jobId = "test-job-123",
      filesCompleted = 7,
      filesRemaining = 3,
      totalFiles = 10,
      failedFiles = 0,
      skippedFiles = 0,
      bytesTransferred = 70_000_000L,
      totalBytes = 100_000_000L,
      currentFileName = "document.pdf",
      currentFileBytes = 5_000_000L,
      currentFileTotalBytes = 10_000_000L,
      transferSpeedBytesPerSec = 10_000_000L,
      etaSeconds = 3L,
      protocol = RealtimeProtocol.WEBSOCKET,
      connectionStatus = RealtimeConnectionStatus.CONNECTED
    )

    assertEquals(7, progress.filesCompleted)
    assertEquals(3, progress.filesRemaining)
    assertEquals(10, progress.totalFiles)
    assertEquals(70_000_000L, progress.bytesTransferred)
    assertEquals(100_000_000L, progress.totalBytes)
    assertEquals("document.pdf", progress.currentFileName)
    assertEquals(0.70f, progress.progressFraction, 0.01f)
    assertEquals(70, progress.progressPercent)
    assertEquals(0.50f, progress.currentFileFraction, 0.01f)
    assertEquals(50, progress.currentFilePercent)
    assertEquals("3s", progress.etaFormatted)
  }

  @Test
  fun testRealtimeProtocolWorkerSwitching() {
    val worker = RealtimeTransferWorker()
    assertEquals(RealtimeProtocol.WEBSOCKET, worker.currentProtocol.value)

    worker.setProtocol(RealtimeProtocol.SSE)
    assertEquals(RealtimeProtocol.SSE, worker.currentProtocol.value)

    worker.setProtocol(RealtimeProtocol.POLLING)
    assertEquals(RealtimeProtocol.POLLING, worker.currentProtocol.value)

    worker.setPollingInterval(500L)
    assertEquals(500L, worker.pollingIntervalMs.value)

    worker.pushProgressUpdate(
      jobId = "job-abc",
      filesCompleted = 2,
      filesRemaining = 8,
      totalFiles = 10,
      failedFiles = 0,
      skippedFiles = 0,
      bytesTransferred = 2_048L,
      totalBytes = 10_240L,
      currentFileName = "photo.jpg",
      currentFileBytes = 1_024L,
      currentFileTotalBytes = 2_048L,
      transferSpeedBytesPerSec = 1_000L,
      etaSeconds = 8L
    )

    val live = worker.liveProgress.value
    assertNotNull(live)
    assertEquals("job-abc", live?.jobId)
    assertEquals(2, live?.filesCompleted)
    assertEquals(8, live?.filesRemaining)
    assertEquals("photo.jpg", live?.currentFileName)
    assertEquals(1_000L, live?.transferSpeedBytesPerSec)
    assertEquals(8L, live?.etaSeconds)
    assertTrue(worker.connectionLogs.value.isNotEmpty())
  }
}

