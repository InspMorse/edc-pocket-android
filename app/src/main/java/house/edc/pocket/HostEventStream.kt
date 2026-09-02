package house.edc.pocket

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HostEventStream {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    fun canUse(health: HostHealth?): Boolean =
        health?.capabilities?.sse == true || health?.capabilities?.websocket == true

    suspend fun listen(
        base: String,
        identity: String,
        onEvent: suspend () -> Unit,
    ) = coroutineScope {
        val root = base.trimEnd('/')
        val url = "$root/api/events?as=${identity.encodeQuery()}"
        while (isActive) {
            val ok = runCatching {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("from", identity)
                    .addHeader("Accept", "text/event-stream")
                    .get()
                    .build()
                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@use false
                    val source = res.body?.source() ?: return@use false
                    while (isActive && !source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data:")) {
                            onEvent()
                        }
                    }
                    true
                }
            }.getOrDefault(false)
            if (!ok) break
            delay(5_000L)
        }
    }

    private fun String.encodeQuery(): String =
        java.net.URLEncoder.encode(this, "UTF-8")
}
