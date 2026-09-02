package house.edc.pocket

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object IncomingFiles {
    fun saveToDevice(context: Context, filename: String, bytes: ByteArray, mime: String): Uri {
        val app = context.applicationContext
        val cleanName = filename.substringAfterLast('/').ifBlank { "edc-file" }
        return if (mime.startsWith("image/")) {
            saveImage(app, cleanName, bytes, mime)
        } else {
            saveDownload(app, cleanName, bytes, mime)
        }
    }

    fun shareBytes(context: Context, filename: String, bytes: ByteArray, mime: String) {
        val app = context.applicationContext
        val dir = File(app.cacheDir, "incoming").apply { mkdirs() }
        val safeName = filename.substringAfterLast('/').ifBlank { "edc-file" }
        val file = File(dir, safeName)
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share file").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(chooser)
    }

    private fun saveImage(context: Context, filename: String, bytes: ByteArray, mime: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EDC")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not save image")
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not write image")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun saveDownload(context: Context, filename: String, bytes: ByteArray, mime: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EDC")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not save file")
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not write file")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
