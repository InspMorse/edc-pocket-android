package house.edc.pocket

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pinDataStore by preferencesDataStore(name = "edc_pins")

class PinStore(private val context: Context) {
    private val clipsKey = stringSetPreferencesKey("clip_keys")
    private val todosKey = stringSetPreferencesKey("todo_ids")

    val pinnedClipKeys: Flow<Set<String>> = context.pinDataStore.data.map { prefs ->
        prefs[clipsKey] ?: emptySet()
    }

    val pinnedTodoIds: Flow<Set<String>> = context.pinDataStore.data.map { prefs ->
        prefs[todosKey] ?: emptySet()
    }

    suspend fun toggleClip(entry: ClipEntry) {
        val key = clipKey(entry)
        context.pinDataStore.edit { prefs ->
            val set = (prefs[clipsKey] ?: emptySet()).toMutableSet()
            if (!set.add(key)) set.remove(key)
            prefs[clipsKey] = set
        }
    }

    suspend fun toggleTodo(id: String) {
        context.pinDataStore.edit { prefs ->
            val set = (prefs[todosKey] ?: emptySet()).toMutableSet()
            if (!set.add(id)) set.remove(id)
            prefs[todosKey] = set
        }
    }

    companion object {
        fun clipKey(entry: ClipEntry): String = "${entry.id}|${entry.ts}"
    }
}
