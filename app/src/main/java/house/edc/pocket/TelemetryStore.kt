package house.edc.pocket

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class TelemetryKind {
    CONNECT_OK,
    CONNECT_FAIL,
    SYNC_OK,
    SYNC_FAIL,
    OUTBOX_SENT,
    OUTBOX_FAIL,
}

data class TelemetryEvent(
    val id: String = UUID.randomUUID().toString(),
    val ts: Long = System.currentTimeMillis(),
    val kind: TelemetryKind,
    val detail: String,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("ts", ts)
        put("kind", kind.name)
        put("detail", detail)
    }
}

private val Context.telemetryStore by preferencesDataStore(name = "edc_telemetry")

class TelemetryStore(private val context: Context) {
    private val eventsKey = stringPreferencesKey("events_json")

    val events: Flow<List<TelemetryEvent>> = context.telemetryStore.data.map { prefs ->
        parseEvents(prefs[eventsKey].orEmpty())
    }

    suspend fun record(kind: TelemetryKind, detail: String, enabled: Boolean) {
        if (!enabled) return
        val safe = detail.take(120).replace(Regex("""https?://[^\s]+"""), "<host>")
        context.telemetryStore.edit { prefs ->
            val current = parseEvents(prefs[eventsKey].orEmpty()).toMutableList()
            current += TelemetryEvent(kind = kind, detail = safe)
            prefs[eventsKey] = toJson(current.takeLast(MAX_EVENTS))
        }
    }

    suspend fun clear() {
        context.telemetryStore.edit { it[eventsKey] = "[]" }
    }

    companion object {
        const val MAX_EVENTS = 200
    }
}

private fun parseEvents(raw: String): List<TelemetryEvent> {
    if (raw.isBlank()) return emptyList()
    val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { obj ->
                add(
                    TelemetryEvent(
                        id = obj.optString("id"),
                        ts = obj.optLong("ts"),
                        kind = runCatching { TelemetryKind.valueOf(obj.optString("kind")) }
                            .getOrDefault(TelemetryKind.CONNECT_FAIL),
                        detail = obj.optString("detail"),
                    ),
                )
            }
        }
    }
}

private fun toJson(events: List<TelemetryEvent>): String {
    val arr = JSONArray()
    events.forEach { arr.put(it.toJson()) }
    return arr.toString()
}

internal fun telemetrySummary(events: List<TelemetryEvent>): String {
    if (events.isEmpty()) return "No telemetry recorded yet."
    val grouped = events.groupingBy { it.kind }.eachCount()
    return grouped.entries.joinToString(" · ") { (kind, count) -> "${kind.name.lowercase()}:$count" }
}
