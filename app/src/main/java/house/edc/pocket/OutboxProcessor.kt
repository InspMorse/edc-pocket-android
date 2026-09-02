package house.edc.pocket

import android.net.Uri
import java.io.File
import kotlinx.coroutines.flow.first

class OutboxProcessor(
    private val client: EdcClient,
    private val outbox: OutboxStore,
) {
    suspend fun flush(settings: EdcSettings): Int {
        val base = settings.baseUrl
        if (base.isBlank()) return 0
        val health = runCatching { client.probeHealth(base, settings.effectiveIdentity) }.getOrNull()
        if (health?.ok != true) return 0
        var sent = 0
        val now = System.currentTimeMillis()
        val pending = outbox.items.first().sortedBy { it.createdAt }
        for (item in pending) {
            if (item.nextRetryAt > now) continue
            val result = runCatching {
                when (item.kind) {
                    OutboxKind.CLIP -> client.sendText(base, settings.effectiveIdentity, item.text)
                    OutboxKind.LIST -> client.addTodo(base, settings.effectiveIdentity, item.text)
                    OutboxKind.PHOTO -> {
                        val file = File(item.imagePath)
                        check(file.exists()) { "Queued photo missing" }
                        client.uploadImage(
                            base,
                            settings.effectiveIdentity,
                            Uri.fromFile(file),
                            item.filename,
                            item.session,
                        )
                    }
                }
            }
            if (result.isSuccess) {
                outbox.remove(item.id)
                sent++
            } else {
                val attempts = item.attemptCount + 1
                val delayMs = retryDelayMs(attempts)
                outbox.updateFailure(
                    id = item.id,
                    attemptCount = attempts,
                    lastError = result.exceptionOrNull()?.message ?: "Send failed",
                    nextRetryAt = now + delayMs,
                )
                break
            }
        }
        return sent
    }

    internal fun retryDelayMs(attempts: Int): Long = outboxRetryDelayMs(attempts)
}

internal fun outboxRetryDelayMs(attempts: Int): Long {
    val capped = attempts.coerceAtMost(6)
    return (5_000L * (1 shl (capped - 1))).coerceAtMost(5 * 60_000L)
}
