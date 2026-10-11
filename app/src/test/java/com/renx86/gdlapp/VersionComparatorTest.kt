package com.renx86.gdlapp

import com.renx86.gdlapp.data.update.VersionComparator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun testHigherMajorVersionIsNewer() {
        assertTrue(VersionComparator.isNewer("v1.0.0", "v0.9.9"))
        assertTrue(VersionComparator.isNewer("2.0.0", "1.99.99"))
        assertFalse(VersionComparator.isNewer("1.0.0", "2.0.0"))
    }

    @Test
    fun testHigherMinorVersionIsNewer() {
        assertTrue(VersionComparator.isNewer("v0.3.0", "v0.2.9"))
        assertFalse(VersionComparator.isNewer("v0.2.3", "v0.3.0"))
    }

    @Test
    fun testHigherPatchVersionIsNewer() {
        assertTrue(VersionComparator.isNewer("v0.2.4", "v0.2.3"))
        assertFalse(VersionComparator.isNewer("v0.2.3", "v0.2.4"))
    }

    @Test
    fun testEqualVersionsAreNotNewer() {
        assertFalse(VersionComparator.isNewer("v0.2.3", "v0.2.3"))
        assertFalse(VersionComparator.isNewer("1.0", "1.0"))
    }

    @Test
    fun testStableReleaseIsNewerThanPreReleaseOfSameVersion() {
        // When user is running v0.2.4-dev, the official v0.2.4 release MUST be considered newer!
        assertTrue(VersionComparator.isNewer("v0.2.4", "v0.2.4-dev"))
        assertTrue(VersionComparator.isNewer("0.2.4", "0.2.4-rc1"))
        assertFalse(VersionComparator.isNewer("v0.2.4-dev", "v0.2.4"))
    }

    @Test
    fun testPreReleaseOrdering() {
        assertTrue(VersionComparator.isNewer("v0.2.4-rc2", "v0.2.4-rc1"))
        assertFalse(VersionComparator.isNewer("v0.2.4-rc1", "v0.2.4-rc2"))
    }

    @Test
    fun testOlderCoreWithPreRelease() {
        // v0.2.3 is older than v0.2.4-dev
        assertTrue(VersionComparator.isNewer("v0.2.4-dev", "v0.2.3"))
        assertFalse(VersionComparator.isNewer("v0.2.3", "v0.2.4-dev"))
    }
}
