package house.edc.pocket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutomationReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val pending = goAsync()
        scope.launch {
            try {
                AutomationActions.handle(context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }
    }
}
