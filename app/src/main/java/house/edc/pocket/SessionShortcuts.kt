package house.edc.pocket

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build

object SessionShortcuts {
    fun sync(context: Context, sessions: List<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val cleaned = sessions.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(4)
        val shortcuts = cleaned.map { session ->
            val slug = session.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            ShortcutInfo.Builder(context, "incoming_$slug")
                .setShortLabel(session.take(10))
                .setLongLabel(context.getString(R.string.session_shortcut_long, session))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_send))
                .setIntent(
                    Intent(context, ShareActivity::class.java).apply {
                        action = Intent.ACTION_SEND
                        type = "image/*"
                        putExtra(EdcIntents.EXTRA_SESSION, session)
                        putExtra(EdcIntents.EXTRA_SKIP_SESSION_PROMPT, true)
                    },
                )
                .build()
        }
        manager.dynamicShortcuts = shortcuts
    }
}
