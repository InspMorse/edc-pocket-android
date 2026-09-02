package house.edc.pocket

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DataExport {
    suspend fun buildJson(
        context: Context,
        settings: EdcSettings,
        syncCache: SyncCache,
        auditLog: AuditLogStore,
        telemetry: TelemetryStore,
        outbox: OutboxStore,
    ): String {
        val hostKey = syncCache.hostKey(settings.baseUrl, settings.effectiveIdentity)
        val snapshot = syncCache.loadSnapshot(hostKey, settings.baseUrl)
        val audit = auditLog.entries.first()
        val telemetryEvents = telemetry.events.first()
        val pending = outbox.items.first()
        return JSONObject().apply {
            put("app_version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            put("exported_at", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("identity", settings.identity)
                put("preset", settings.preset.name)
                put("base_url", settings.baseUrl)
                put("active_profile", settings.activeProfileId)
                put("telemetry_opt_in", settings.telemetryOptIn)
            })
            put("cached_snapshot", JSONObject().apply {
                put("clip_count", snapshot?.history?.size ?: 0)
                put("todo_count", snapshot?.todos?.size ?: 0)
                put("incoming_count", snapshot?.drops?.size ?: 0)
                put("last_synced_at", syncCache.getLastSynced(hostKey) ?: 0L)
            })
            put("audit_log", JSONArray().apply {
                audit.forEach { put(it.toJson()) }
            })
            put("telemetry", JSONArray().apply {
                telemetryEvents.forEach { put(it.toJson()) }
            })
            put("outbox_pending", pending.size)
        }.toString(2)
    }

    fun shareJson(context: Context, json: String, filename: String = "edc-pocket-export.json") {
        val app = context.applicationContext
        val dir = File(app.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, filename)
        file.writeText(json)
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Export EDC pocket data").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(chooser)
    }
}
