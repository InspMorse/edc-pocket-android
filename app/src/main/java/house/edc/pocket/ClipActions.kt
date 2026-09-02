package house.edc.pocket

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.first

object ClipActions {
    suspend fun copyHouseClipboard(context: Context): String? {
        val app = context.applicationContext
        val store = LatestClipStore(app)
        store.peek()?.text?.takeIf { it.isNotBlank() }?.let { cached ->
            copyPlain(app, cached)
            return cached
        }
        val settings = SettingsStore(app).settings.first()
        val base = settings.baseUrl
        if (base.isBlank()) return null
        val client = EdcClient(app.contentResolver)
        val snap = runCatching { client.load(base, settings.effectiveIdentity) }.getOrNull() ?: return null
        val entry = snap.latest ?: snap.history.firstOrNull() ?: return null
        store.save(entry)
        SurfaceEffects.afterClipSaved(app, settings)
        copyPlain(app, entry.text)
        return entry.text
    }

    fun copyPlain(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("EDC", text))
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
