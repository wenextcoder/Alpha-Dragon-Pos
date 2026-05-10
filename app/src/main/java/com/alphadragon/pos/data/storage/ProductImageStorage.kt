package com.alphadragon.pos.data.storage

import android.content.Context
import android.net.Uri
import com.alphadragon.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies product images into the app's private internal storage.
 *
 * Storage location: [Context.filesDir]/product_images/
 *
 * This directory is:
 *   - Sandboxed to this app's UID — other apps cannot read it
 *   - Not accessible via the Android file manager or MTP
 *   - Included in Android backup (controlled by backup_rules.xml)
 *   - Persistent across launches (unlike cacheDir which the OS may purge)
 *
 * Images are stored as JPEG with a UUID filename so there are no name collisions
 * and no information about the original file source is retained.
 */
@Singleton
class ProductImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dir: File
        get() = File(context.filesDir, "product_images").also { it.mkdirs() }

    /**
     * Copies the image at [sourceUri] into private storage.
     * Returns the absolute path to the copied file, or null on failure.
     * Any previously stored file at [replacingPath] is deleted first.
     */
    suspend fun copyFromUri(sourceUri: Uri, replacingPath: String? = null): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                // Delete old image before writing the replacement
                if (replacingPath != null) deleteFile(replacingPath)

                val dest = File(dir, "${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest.absolutePath
            }.getOrElse { e ->
                Logger.e("Failed to copy product image", e, "ProductImageStorage")
                null
            }
        }

    /**
     * Deletes the file at [path] if it lives inside our private product_images dir.
     * Silently ignores paths that belong to other locations (e.g. old content:// URIs).
     */
    fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists() && file.canonicalPath.startsWith(dir.canonicalPath)) {
            file.delete()
        }
    }
}
