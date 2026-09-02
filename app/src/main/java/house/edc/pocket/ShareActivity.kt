package house.edc.pocket

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shareIntent = intent
        lifecycleScope.launch {
            val settings = SettingsStore(this@ShareActivity).settings.first()
            if (settings.baseUrl.isBlank()) {
                toast("Set a host URL first")
                finish()
                return@launch
            }
            try {
                when {
                    shareIntent.type?.startsWith("image/") == true -> {
                        val uri = shareIntent.parcelUri() ?: error("No image")
                        val name = uri.lastPathSegment ?: "photo.jpg"
                        withContext(Dispatchers.IO) {
                            EdcClient(contentResolver).uploadImage(
                                settings.baseUrl,
                                settings.identity,
                                uri,
                                name,
                            )
                        }
                        toast("Photo sent to Incoming")
                        finish()
                    }
                    else -> {
                        val text = shareIntent.resolveText()
                        if (text.isBlank()) error("Nothing to send")
                        showDestinationChooser(text, settings)
                    }
                }
            } catch (e: Exception) {
                toast(e.message ?: "Send failed")
                finish()
            }
        }
    }

    private suspend fun showDestinationChooser(text: String, settings: EdcSettings) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(this@ShareActivity)
                .setTitle("Send to EDC")
                .setItems(arrayOf("House clipboard", "To-do list")) { _, which ->
                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val client = EdcClient(contentResolver)
                                when (which) {
                                    0 -> client.sendText(settings.baseUrl, settings.identity, text)
                                    else -> client.addTodo(settings.baseUrl, settings.identity, text)
                                }
                            }
                            toast(if (which == 0) "Sent to clipboard" else "Added to list")
                        } catch (e: Exception) {
                            toast(e.message ?: "Send failed")
                        }
                        finish()
                    }
                }
                .setOnCancelListener { finish() }
                .show()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Intent.resolveText(): String {
        val direct = getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (direct.isNotEmpty()) return direct
        val stream = parcelUri() ?: return ""
        return contentResolver.openInputStream(stream)?.bufferedReader()?.use { it.readText() }
            ?.trim()
            .orEmpty()
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
