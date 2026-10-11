package com.renx86.gdlapp

import com.renx86.gdlapp.data.db.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStatusTest {

    @Test
    fun testStatusValues() {
        assertEquals("QUEUED", DownloadStatus.QUEUED)
        assertEquals("DOWNLOADING", DownloadStatus.DOWNLOADING)
        assertEquals("COMPLETED", DownloadStatus.COMPLETED)
        assertEquals("FAILED", DownloadStatus.FAILED)
        assertEquals("CANCELLED", DownloadStatus.CANCELLED)
    }

    @Test
    fun testTerminalStates() {
        val terminalStates = setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
        assertTrue(terminalStates.contains(DownloadStatus.COMPLETED))
        assertTrue(terminalStates.contains(DownloadStatus.FAILED))
        assertTrue(terminalStates.contains(DownloadStatus.CANCELLED))
    }
}
