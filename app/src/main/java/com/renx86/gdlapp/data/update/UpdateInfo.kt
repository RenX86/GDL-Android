package com.renx86.gdlapp.data.update

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val changelog: String,
    val htmlUrl: String
)
