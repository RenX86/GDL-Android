package com.renx86.gdlapp.data.engine

/**
 * Metadata for an upstream gallery-dl release from PyPI.
 */
data class EngineUpdateInfo(
    val version: String,
    val wheelUrl: String,
    val sizeBytes: Long,
    val sha256: String,
    val releaseDate: String
)
