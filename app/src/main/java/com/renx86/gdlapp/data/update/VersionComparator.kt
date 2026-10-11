package com.renx86.gdlapp.data.update

object VersionComparator {
    fun isNewer(latest: String, current: String): Boolean {
        val lClean = latest.removePrefix("v").trim()
        val cClean = current.removePrefix("v").trim()

        val lCore = lClean.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        val cCore = cClean.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(lCore.size, cCore.size)
        for (i in 0 until maxLen) {
            val l = lCore.getOrElse(i) { 0 }
            val c = cCore.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }

        // Core versions are equal. In SemVer, 0.2.4 (stable) is newer than 0.2.4-dev
        val lPre = if (lClean.contains("-")) lClean.substringAfter("-") else null
        val cPre = if (cClean.contains("-")) cClean.substringAfter("-") else null

        return when {
            lPre == null && cPre != null -> true   // latest is stable, current is pre-release -> newer!
            lPre != null && cPre == null -> false  // latest is pre-release, current is stable -> not newer
            lPre != null && cPre != null -> lPre > cPre // both pre-releases, e.g. rc2 > rc1
            else -> false
        }
    }
}
