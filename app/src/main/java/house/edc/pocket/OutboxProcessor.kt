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
        val health = runCatching { client.probeHealth(base, settings.identity) }.getOrNull()
        if (health?.ok != true) return 0
        var sent = 0
        val pending = outbox.items.first()
        for (item in pending) {
            val ok = runCatching {
                when (item.kind) {
                    OutboxKind.CLIP -> client.sendText(base, settings.identity, item.text)
                    OutboxKind.LIST -> client.addTodo(base, settings.identity, item.text)
                    OutboxKind.PHOTO -> {
                        val file = File(item.imagePath)
                        check(file.exists()) { "Queued photo missing" }
                        client.uploadImage(
                            base,
                            settings.identity,
                            Uri.fromFile(file),
                            item.filename,
                            item.session,
                        )
                    }
                }
            }.isSuccess
            if (!ok) break
            outbox.remove(item.id)
            sent++
        }
        return sent
    }
}
