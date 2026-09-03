package house.edc.pocket

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.auditLogStore by preferencesDataStore(name = "edc_audit_log")

class AuditLogStore(private val context: Context) {
    private val entriesKey = stringPreferencesKey("entries_json")

    val entries: Flow<List<AuditEntry>> = context.auditLogStore.data.map { prefs ->
        auditEntriesFromJson(prefs[entriesKey].orEmpty())
    }

    suspend fun append(
        kind: AuditKind,
        identity: String,
        detail: String,
        success: Boolean,
    ) {
        context.auditLogStore.edit { prefs ->
            val current = auditEntriesFromJson(prefs[entriesKey].orEmpty()).toMutableList()
            current += AuditEntry(
                kind = kind,
                identity = identity,
                detail = detail,
                success = success,
            )
            val trimmed = if (current.size > MAX_ENTRIES) {
                current.takeLast(MAX_ENTRIES)
            } else {
                current
            }
            prefs[entriesKey] = auditEntriesToJson(trimmed)
        }
    }

    suspend fun clear() {
        context.auditLogStore.edit { it[entriesKey] = "[]" }
    }

    companion object {
        const val MAX_ENTRIES = 500
    }
}
