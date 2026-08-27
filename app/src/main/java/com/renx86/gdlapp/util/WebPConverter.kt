package com.renx86.gdlapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object WebPConverter {

    /**
     * Converts a given image file (JPG, PNG) to WebP.
     * Replaces the original file with the new .webp file on success unless keepOriginal is true.
     *
     * @param imageFile The input file
     * @param quality The compression quality (0-100)
     * @param keepOriginal If true, do not delete the original file
     * @return The new WebP file, or the original file if conversion fails or isn't supported.
     */
    fun convertFileToWebp(imageFile: File, quality: Int, keepOriginal: Boolean = false): File {
        val ext = imageFile.extension.lowercase()
        // We only convert jpg/jpeg/png natively via Bitmap. Skip videos/gifs to avoid crashes.
        if (ext !in listOf("jpg", "jpeg", "png")) {
            return imageFile
        }

        val outName = imageFile.nameWithoutExtension + ".webp"
        val outWebpFile = File(imageFile.parentFile, outName)

        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: return imageFile // Failed to decode

            FileOutputStream(outWebpFile).use { out ->
                // Native WebP compression
                // On Android 11+ (API 30+), WEBP_LOSSY is preferred. For older devices, WEBP works.
                @Suppress("DEPRECATION")
                val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(format, quality.coerceIn(0, 100), out)
            }
            bitmap.recycle()

            if (outWebpFile.exists() && outWebpFile.length() > 0) {
                if (!keepOriginal) {
                    imageFile.delete()
                }
                return outWebpFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            if (outWebpFile.exists()) outWebpFile.delete()
        }
        
        return imageFile
    }
}
