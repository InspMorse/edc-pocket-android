package house.edc.pocket

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AuditKind(val label: String) {
    SEND_CLIP("Send clip"),
    SEND_LIST("Send list"),
    UPLOAD("Upload"),
    OUTBOX("Outbox"),
    SYNC("Sync"),
    HOST_ERROR("Host error"),
    RATE_LIMIT("Rate limit"),
    RESET("Reset"),
}

data class AuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val ts: Long = System.currentTimeMillis(),
    val kind: AuditKind,
    val identity: String,
    val detail: String,
    val success: Boolean,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("ts", ts)
        put("kind", kind.name)
        put("identity", identity)
        put("detail", detail)
        put("success", success)
    }

    fun formatLine(): String {
        val status = if (success) "ok" else "fail"
        return "${java.time.Instant.ofEpochMilli(ts)} · ${kind.label} · $identity · $status · $detail"
    }

    companion object {
        internal fun fromJson(obj: JSONObject): AuditEntry = AuditEntry(
            id = obj.optString("id", UUID.randomUUID().toString()),
            ts = obj.optLong("ts", System.currentTimeMillis()),
            kind = runCatching { AuditKind.valueOf(obj.optString("kind")) }
                .getOrDefault(AuditKind.HOST_ERROR),
            identity = obj.optString("identity"),
            detail = obj.optString("detail"),
            success = obj.optBoolean("success", false),
        )
    }
}

internal fun auditEntriesFromJson(raw: String): List<AuditEntry> {
    if (raw.isBlank()) return emptyList()
    val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { add(AuditEntry.fromJson(it)) }
        }
    }
}

internal fun auditEntriesToJson(entries: List<AuditEntry>): String {
    val arr = JSONArray()
    entries.forEach { arr.put(it.toJson()) }
    return arr.toString()
}

internal fun formatAuditExport(entries: List<AuditEntry>): String =
    entries.joinToString("\n") { it.formatLine() }
