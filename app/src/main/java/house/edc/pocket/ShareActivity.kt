package house.edc.pocket

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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
                    shareIntent.action == Intent.ACTION_SEND_MULTIPLE &&
                        shareIntent.type?.startsWith("image/") == true -> {
                        val uris = shareIntent.imageUris()
                        if (uris.isEmpty()) error("No images")
                        val session = promptSession() ?: run {
                            finish()
                            return@launch
                        }
                        sendPhotos(client, outboxStore, settings, uris, session)
                        finish()
                    }
                    shareIntent.type?.startsWith("image/") == true -> {
                        val uris = shareIntent.imageUris()
                        val uri = uris.firstOrNull() ?: error("No image")
                        val session = promptSession() ?: run {
                            finish()
                            return@launch
                        }
                        sendPhotos(client, outboxStore, settings, listOf(uri), session)
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

    private suspend fun promptSession(): String? = suspendCancellableCoroutine { cont ->
        val input = EditText(this).apply {
            hint = getString(R.string.session_folder_hint)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.session_folder_title)
            .setView(input)
            .setPositiveButton(R.string.send_to_incoming) { _, _ ->
                if (cont.isActive) cont.resume(input.text?.toString().orEmpty())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                if (cont.isActive) cont.resume(null)
            }
            .setOnCancelListener {
                if (cont.isActive) cont.resume(null)
            }
            .show()
    }

    private suspend fun sendPhotos(
        client: EdcClient,
        outboxStore: OutboxStore,
        settings: EdcSettings,
        uris: List<Uri>,
        session: String,
    ) {
        var sent = 0
        var queued = 0
        uris.forEachIndexed { index, uri ->
            val name = uri.lastPathSegment ?: "photo_${index + 1}.jpg"
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    client.uploadImage(settings.baseUrl, settings.identity, uri, name, session)
                }
            }
            if (result.isSuccess) {
                sent++
            } else {
                withContext(Dispatchers.IO) {
                    outboxStore.enqueuePhoto(contentResolver, uri, name, session)
                }
                queued++
            }
        }
        toast(
            when {
                sent == uris.size -> if (sent == 1) "Photo sent to Incoming" else "$sent photos sent"
                queued == uris.size -> "Queued ${uris.size} photo(s)"
                else -> "Sent $sent, queued $queued"
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
}
