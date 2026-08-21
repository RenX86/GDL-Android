package com.renx86.gdlapp.model

import java.util.UUID

enum class DownloadStatus {
    QUEUED, DOWNLOADING, DONE, FAILED
}

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val filename: String = "",
    val error: String = ""
)