package com.renx86.gdlapp

import com.renx86.gdlapp.data.engine.GalleryDlUpdateManager
import com.renx86.gdlapp.data.update.VersionComparator
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GalleryDlUpdateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testParsePyPiJson() {
        val json = """
        {
            "info": {
                "version": "1.28.5"
            },
            "urls": [
                {
                    "packagetype": "sdist",
                    "filename": "gallery_dl-1.28.5.tar.gz",
                    "url": "https://files.pythonhosted.org/packages/source/gallery-dl.tar.gz",
                    "size": 500000,
                    "digests": {
                        "sha256": "tar_sha256"
                    }
                },
                {
                    "packagetype": "bdist_wheel",
                    "filename": "gallery_dl-1.28.5-py3-none-any.whl",
                    "url": "https://files.pythonhosted.org/packages/wheel/gallery_dl-1.28.5-py3-none-any.whl",
                    "size": 654321,
                    "digests": {
                        "sha256": "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
                    },
                    "upload_time": "2026-10-10T12:00:00"
                }
            ]
        }
        """.trimIndent()

        val info = GalleryDlUpdateManager.parsePyPiJson(json)
        assertNotNull("Should successfully parse wheel info", info)
        assertEquals("1.28.5", info?.version)
        assertEquals("https://files.pythonhosted.org/packages/wheel/gallery_dl-1.28.5-py3-none-any.whl", info?.wheelUrl)
        assertEquals(654321L, info?.sizeBytes)
        assertEquals("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", info?.sha256)
        assertEquals("2026-10-10T12:00:00", info?.releaseDate)
    }

    @Test
    fun testZipSlipProtectionThrowsException() {
        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zos ->
            zos.putNextEntry(ZipEntry("../../malicious.txt"))
            zos.write("evil content".toByteArray())
            zos.closeEntry()
        }

        val destination = tempFolder.newFolder("safe_dir")
        try {
            GalleryDlUpdateManager.unzipWithSlipProtection(
                ByteArrayInputStream(byteOut.toByteArray()),
                destination
            )
            fail("Expected SecurityException on Zip Slip entry")
        } catch (e: SecurityException) {
            assertTrue("SecurityException should mention Zip Slip or traversal", e.message?.contains("Zip Slip") == true)
        }
    }

    @Test
    fun testZipExtractionValid() {
        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zos ->
            zos.putNextEntry(ZipEntry("gallery_dl/__init__.py"))
            zos.write("__version__ = '1.28.5'".toByteArray())
            zos.closeEntry()
        }

        val destination = tempFolder.newFolder("unpacked_engine")
        GalleryDlUpdateManager.unzipWithSlipProtection(
            ByteArrayInputStream(byteOut.toByteArray()),
            destination
        )

        val extractedInit = File(destination, "gallery_dl/__init__.py")
        assertTrue("Extracted __init__.py should exist", extractedInit.exists())
        assertEquals("__version__ = '1.28.5'", extractedInit.readText())
    }

    @Test
    fun testVersionComparatorWithEngineVersions() {
        assertTrue("Newer version should be recognized", VersionComparator.isNewer("1.28.5", "1.28.4"))
        assertTrue("Major bump should be recognized", VersionComparator.isNewer("1.29.0", "1.28.9"))
        assertFalse("Same version should not be newer", VersionComparator.isNewer("1.28.4", "1.28.4"))
        assertFalse("Older version should not be newer", VersionComparator.isNewer("1.27.0", "1.28.4"))
    }
}
