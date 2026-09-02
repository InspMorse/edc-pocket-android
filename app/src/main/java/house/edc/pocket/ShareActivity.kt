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
            val store = SettingsStore(this@ShareActivity)
            val outboxStore = OutboxStore(this@ShareActivity)
            val settings = store.settings.first()
            if (settings.baseUrl.isBlank()) {
                toast("Set a host URL first")
                finish()
                return@launch
            }
            val client = EdcClient(contentResolver)
            try {
                when {
                    shareIntent.type?.startsWith("image/") == true -> {
                        val uri = shareIntent.parcelUri() ?: error("No image")
                        val name = uri.lastPathSegment ?: "photo.jpg"
                        sendPhoto(client, outboxStore, settings, uri, name)
                        finish()
                    }
                    else -> {
                        val text = shareIntent.resolveText()
                        if (text.isBlank()) error("Nothing to send")
                        showDestinationChooser(text, settings, client, outboxStore)
                    }
                }
            } catch (e: Exception) {
                toast(e.message ?: "Send failed")
                finish()
            }
        }
    }

    private suspend fun sendPhoto(
        client: EdcClient,
        outboxStore: OutboxStore,
        settings: EdcSettings,
        uri: Uri,
        name: String,
    ) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                client.uploadImage(settings.baseUrl, settings.identity, uri, name)
            }
        }
        result.fold(
            onSuccess = { toast("Photo sent to Incoming") },
            onFailure = {
                withContext(Dispatchers.IO) {
                    outboxStore.enqueuePhoto(contentResolver, uri, name)
                }
                toast("Queued photo for when host is back")
            },
        )
    }

    private suspend fun showDestinationChooser(
        text: String,
        settings: EdcSettings,
        client: EdcClient,
        outboxStore: OutboxStore,
    ) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(this@ShareActivity)
                .setTitle("Send to EDC")
                .setItems(arrayOf("House clipboard", "To-do list")) { _, which ->
                    lifecycleScope.launch {
                        val kind = if (which == 0) OutboxKind.CLIP else OutboxKind.LIST
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                when (kind) {
                                    OutboxKind.CLIP -> client.sendText(
                                        settings.baseUrl,
                                        settings.identity,
                                        text,
                                    )
                                    OutboxKind.LIST -> client.addTodo(
                                        settings.baseUrl,
                                        settings.identity,
                                        text,
                                    )
                                    OutboxKind.PHOTO -> error("Unexpected")
                                }
                            }
                        }
                        result.fold(
                            onSuccess = {
                                toast(
                                    if (which == 0) "Sent to clipboard" else "Added to list",
                                )
                            },
                            onFailure = {
                                withContext(Dispatchers.IO) {
                                    outboxStore.enqueue(
                                        OutboxItem(kind = kind, text = text),
                                    )
                                }
                                toast("Queued for when host is back")
                            },
                        )
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
