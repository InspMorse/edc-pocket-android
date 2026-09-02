package house.edc.pocket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

class CopyClipReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != EdcIntents.ACTION_COPY_CLIP) return
        val pending = goAsync()
        runBlocking {
            val text = ClipActions.copyHouseClipboard(context)
            if (text.isNullOrBlank()) {
                ClipActions.toast(context, "Nothing to copy")
            } else {
                ClipActions.toast(context, "Copied house clip")
            }
            pending.finish()
        }
    }
}
