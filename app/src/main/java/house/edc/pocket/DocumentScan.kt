package house.edc.pocket

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

object DocumentScan {
    fun start(activity: Activity, onReady: (IntentSender) -> Unit, onError: (String) -> Unit) {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener(onReady)
            .addOnFailureListener { onError(it.message ?: "Scanner unavailable") }
    }

    fun readPages(context: Context, data: Intent?): List<Pair<String, ByteArray>> {
        val result = GmsDocumentScanningResult.fromActivityResultIntent(data) ?: return emptyList()
        val pages = result.pages ?: return emptyList()
        return pages.mapIndexedNotNull { index, page ->
            val uri = page.imageUri ?: return@mapIndexedNotNull null
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@mapIndexedNotNull null
            "scan_${index + 1}.jpg" to bytes
        }
    }
}
