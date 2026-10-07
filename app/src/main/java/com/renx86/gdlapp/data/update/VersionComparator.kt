package com.renx86.gdlapp.data.update

object VersionComparator {
    fun isNewer(latest: String, current: String): Boolean {
        val lParts = latest.removePrefix("v").split(".", "-").map { it.toIntOrNull() ?: 0 }
        val cParts = current.removePrefix("v").split(".", "-").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(lParts.size, cParts.size)
        for (i in 0 until length) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false // Identical
    }
}
