package house.edc.pocket

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class TodoSubItem(
    val text: String,
    val done: Boolean = false,
)

enum class RecurrenceRule(val label: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ;

    companion object {
        fun from(raw: String): RecurrenceRule = entries.find {
            it.name.equals(raw.trim(), ignoreCase = true) ||
                it.label.equals(raw.trim(), ignoreCase = true)
        } ?: NONE
    }
}

data class TodoExtra(
    val note: String = "",
    val dueDate: String = "",
    val category: String = "",
    val linkedClipUrl: String = "",
    val linkedClipText: String = "",
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val subItems: List<TodoSubItem> = emptyList(),
)

internal fun mergeTodoExtra(item: TodoItem, extra: TodoExtra?): TodoItem {
    if (extra == null) return item
    return item.copy(
        note = item.note.ifBlank { extra.note },
        dueDate = item.dueDate.ifBlank { extra.dueDate },
        category = item.category.ifBlank { extra.category },
        linkedClipUrl = item.linkedClipUrl.ifBlank { extra.linkedClipUrl },
        linkedClipText = item.linkedClipText.ifBlank { extra.linkedClipText },
        recurrence = if (item.recurrence.isBlank()) extra.recurrence.name else item.recurrence,
        subItems = if (item.subItems.isEmpty()) extra.subItems else item.subItems,
    )
}

internal fun enrichTodos(items: List<TodoItem>, extras: Map<String, TodoExtra>): List<TodoItem> =
    items.map { mergeTodoExtra(it, extras[it.id]) }

private val Context.todoExtrasStore by preferencesDataStore(name = "edc_todo_extras")

class TodoExtrasStore(private val context: Context) {
    private val key = stringPreferencesKey("extras_json")

    val extras: Flow<Map<String, TodoExtra>> = context.todoExtrasStore.data.map { prefs ->
        parseExtrasJson(prefs[key].orEmpty())
    }

    suspend fun save(id: String, extra: TodoExtra) {
        context.todoExtrasStore.edit { prefs ->
            val map = parseExtrasJson(prefs[key].orEmpty()).toMutableMap()
            map[id] = extra
            prefs[key] = extrasToJson(map)
        }
    }

    suspend fun remove(id: String) {
        context.todoExtrasStore.edit { prefs ->
            val map = parseExtrasJson(prefs[key].orEmpty()).toMutableMap()
            map.remove(id)
            prefs[key] = extrasToJson(map)
        }
    }
}

internal fun parseExtrasJson(raw: String): Map<String, TodoExtra> {
    if (raw.isBlank()) return emptyMap()
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
    return buildMap {
        root.keys().forEach { id ->
            root.optJSONObject(id)?.let { put(id, it.toTodoExtra()) }
        }
    }
}

private fun extrasToJson(map: Map<String, TodoExtra>): String {
    val root = JSONObject()
    map.forEach { (id, extra) -> root.put(id, extra.toJson()) }
    return root.toString()
}

private fun TodoExtra.toJson(): JSONObject = JSONObject().apply {
    put("note", note)
    put("dueDate", dueDate)
    put("category", category)
    put("linkedClipUrl", linkedClipUrl)
    put("linkedClipText", linkedClipText)
    put("recurrence", recurrence.name)
    val subs = JSONArray()
    subItems.forEach { sub ->
        subs.put(JSONObject().apply {
            put("text", sub.text)
            put("done", sub.done)
        })
    }
    put("subItems", subs)
}

private fun JSONObject.toTodoExtra(): TodoExtra {
    val subs = buildList {
        optJSONArray("subItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { obj ->
                    add(
                        TodoSubItem(
                            text = obj.optString("text"),
                            done = obj.optBoolean("done", false),
                        ),
                    )
                }
            }
        }
    }
    return TodoExtra(
        note = optString("note"),
        dueDate = optString("dueDate", optString("due_date")),
        category = optString("category", optString("aisle")),
        linkedClipUrl = optString("linkedClipUrl", optString("linked_clip_url")),
        linkedClipText = optString("linkedClipText", optString("linked_clip_text")),
        recurrence = RecurrenceRule.from(optString("recurrence")),
        subItems = subs,
    )
}

internal fun recurrenceFollowUpText(text: String, rule: RecurrenceRule): String? = when (rule) {
    RecurrenceRule.NONE -> null
    else -> text.trim().takeIf { it.isNotEmpty() }
}
