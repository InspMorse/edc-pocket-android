package house.edc.pocket

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.outboxDataStore by preferencesDataStore(name = "edc_outbox")

class OutboxStore(private val context: Context) {
    private val itemsKey = stringPreferencesKey("items_json")

    val items: Flow<List<OutboxItem>> = context.outboxDataStore.data.map { prefs ->
        OutboxItem.listFromJson(prefs[itemsKey].orEmpty())
    }

    suspend fun enqueue(item: OutboxItem) {
        context.outboxDataStore.edit { prefs ->
            val current = OutboxItem.listFromJson(prefs[itemsKey].orEmpty())
            prefs[itemsKey] = OutboxItem.listToJson(current + item)
        }
    }

    suspend fun remove(id: String) {
        context.outboxDataStore.edit { prefs ->
            val current = OutboxItem.listFromJson(prefs[itemsKey].orEmpty())
            val removed = current.filter { it.id != id }
            current.firstOrNull { it.id == id }?.imagePath
                ?.takeIf { it.isNotBlank() }
                ?.let { File(it).delete() }
            prefs[itemsKey] = OutboxItem.listToJson(removed)
        }
    }

    suspend fun clear() {
        val current = items.first()
        current.forEach { item ->
            if (item.imagePath.isNotBlank()) File(item.imagePath).delete()
        }
        context.outboxDataStore.edit { it[itemsKey] = "[]" }
    }

    suspend fun enqueuePhoto(
        resolver: ContentResolver,
        uri: Uri,
        filename: String,
        session: String = "",
    ): OutboxItem {
        val dir = File(context.filesDir, "outbox/images").apply { mkdirs() }
        val id = java.util.UUID.randomUUID().toString()
        val ext = filename.substringAfterLast('.', "jpg")
        val file = File(dir, "$id.$ext")
        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read image")
        val item = OutboxItem(
            id = id,
            kind = OutboxKind.PHOTO,
            filename = filename,
            session = session,
            imagePath = file.absolutePath,
            mime = resolver.getType(uri) ?: "image/jpeg",
        )
        enqueue(item)
        return item
    }
}
