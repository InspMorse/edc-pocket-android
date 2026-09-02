package house.edc.pocket

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first

object AutomationActions {
    suspend fun handle(context: Context, intent: Intent) {
        when (intent.action) {
            EdcIntents.ACTION_AUTOMATION_COPY -> {
                val text = ClipActions.copyHouseClipboard(context)
                if (text.isNullOrBlank()) {
                    ClipActions.toast(context, "Could not copy clip")
                } else {
                    ClipActions.toast(context, "Copied house clip")
                }
            }
            EdcIntents.ACTION_AUTOMATION_SEND_CLIP,
            EdcIntents.ACTION_AUTOMATION_SEND_LIST,
            -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
                if (text.isBlank()) {
                    ClipActions.toast(context, "No text to send")
                    return
                }
                val kind = when (intent.action) {
                    EdcIntents.ACTION_AUTOMATION_SEND_LIST -> OutboxKind.LIST
                    else -> OutboxKind.CLIP
                }
                sendText(context, kind, text)
            }
            EdcIntents.ACTION_AUTOMATION_OPEN_LIST -> {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        action = ACTION_OPEN_LIST
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
                )
            }
            EdcIntents.ACTION_AUTOMATION_OPEN_SEND -> {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        action = ACTION_OPEN_SEND
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
                )
            }
        }
    }

    private suspend fun sendText(context: Context, kind: OutboxKind, text: String) {
        val store = SettingsStore(context)
        val settings = store.settings.first()
        val base = settings.baseUrl
        if (base.isBlank()) {
            ClipActions.toast(context, "Set a host URL first")
            return
        }
        val client = EdcClient(context.contentResolver)
        val outbox = OutboxStore(context)
        val ok = runCatching {
            when (kind) {
                OutboxKind.CLIP -> client.sendText(base, settings.effectiveIdentity, text)
                OutboxKind.LIST -> client.addTodo(base, settings.effectiveIdentity, text)
                OutboxKind.PHOTO -> error("Unexpected")
            }
        }.isSuccess
        if (ok) {
            ClipActions.toast(
                context,
                if (kind == OutboxKind.CLIP) "Sent to clipboard" else "Added to list",
            )
        } else {
            outbox.enqueue(OutboxItem(kind = kind, text = text))
            ClipActions.toast(context, "Queued for when host is back")
        }
    }
}
