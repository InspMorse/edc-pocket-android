package house.edc.pocket

import android.content.Context
import android.content.Intent

class WidgetSnapshotStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun openTodoCount(): Int = prefs.getInt(KEY_OPEN_TODOS, 0)

    fun showTodoCount(): Boolean = prefs.getBoolean(KEY_SHOW_TODO, true)

    fun tapAction(): WidgetTapAction = runCatching {
        WidgetTapAction.valueOf(prefs.getString(KEY_TAP_ACTION, WidgetTapAction.OPEN_APP.name)!!)
    }.getOrDefault(WidgetTapAction.OPEN_APP)

    fun clipFilter(): String = prefs.getString(KEY_CLIP_FILTER, "All").orEmpty()

    fun updateFromSnapshot(snapshot: HostSnapshot) {
        prefs.edit()
            .putInt(KEY_OPEN_TODOS, snapshot.todos.count { !it.done })
            .apply()
    }

    fun syncFromSettings(settings: EdcSettings) {
        prefs.edit()
            .putBoolean(KEY_SHOW_TODO, settings.widgetShowTodoCount)
            .putString(KEY_TAP_ACTION, settings.widgetTapAction.name)
            .putString(KEY_CLIP_FILTER, settings.clipFilter)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun openIntent(context: Context): Intent {
        val base = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return when (tapAction()) {
            WidgetTapAction.OPEN_LIST -> base.apply { action = ACTION_OPEN_LIST }
            WidgetTapAction.OPEN_CLIP -> base.apply {
                action = ACTION_OPEN_CLIP
                putExtra(EdcIntents.EXTRA_CLIP_FILTER, clipFilter())
            }
            else -> base
        }
    }

    companion object {
        private const val PREFS = "widget_snapshot"
        private const val KEY_OPEN_TODOS = "open_todos"
        private const val KEY_SHOW_TODO = "show_todo"
        private const val KEY_TAP_ACTION = "tap_action"
        private const val KEY_CLIP_FILTER = "clip_filter"
    }
}
