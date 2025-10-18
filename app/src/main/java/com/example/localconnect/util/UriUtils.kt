package com.example.localconnect.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

object UriUtils {
    /**
     * Copies content from the provided Uri into a temp file in the app cache and returns it.
     * The caller is responsible for deleting the file when no longer needed.
     */
    fun uriToFile(context: Context, uri: Uri, prefix: String = "upload_"): File {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open input stream for uri: $uri")

        // Determine proper file extension based on MIME type
        val mimeType = context.contentResolver.getType(uri)
        val extension = when {
            mimeType?.startsWith("video/") == true -> {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "mp4"
            }
            mimeType?.startsWith("image/") == true -> {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            }
            else -> "tmp"
        }

        val file = File.createTempFile(
            prefix + System.currentTimeMillis().toString(),
            ".$extension",
            context.cacheDir
        )

        input.use { inputStream ->
            file.outputStream().use { out ->
                inputStream.copyTo(out)
            }
        }

        return file
    }
}
