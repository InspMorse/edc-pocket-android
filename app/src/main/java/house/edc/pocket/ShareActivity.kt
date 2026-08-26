package house.edc.pocket

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        CoroutineScope(Dispatchers.IO).launch {
            val settings = SettingsStore(this@ShareActivity).settings.first()
            val client = EdcClient(contentResolver)
            try {
                if (settings.baseUrl.isBlank()) error("Set a host URL first")
                when {
                    intent.type?.startsWith("image/") == true -> {
                        val uri = intent.parcelUri() ?: error("No image")
                        val name = uri.lastPathSegment ?: "photo.jpg"
                        client.uploadImage(settings.baseUrl, settings.identity, uri, name)
                    }
                    else -> {
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
                        if (text.isEmpty()) error("Nothing to send")
                        client.sendText(settings.baseUrl, settings.identity, text)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareActivity, "Sent to EDC", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ShareActivity,
                        e.message ?: "Send failed",
                        Toast.LENGTH_LONG,
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun Intent.parcelUri(): Uri? {
        val extra = extras ?: return null
        val stream = if (android.os.Build.VERSION.SDK_INT >= 33) {
            extra.getParcelable(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            extra.getParcelable(Intent.EXTRA_STREAM) as? Uri
        }
        return stream ?: data
    }
}
