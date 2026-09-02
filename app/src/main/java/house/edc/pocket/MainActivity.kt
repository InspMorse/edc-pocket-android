package house.edc.pocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val launchActionState = mutableStateOf(LaunchAction.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EdcNotifications.ensureChannel(this)
        launchActionState.value = intent.launchAction()
        val store = SettingsStore(this)
        val outboxStore = OutboxStore(this)
        val client = EdcClient(contentResolver)
        val outboxProcessor = OutboxProcessor(client, outboxStore)
        val hostConnector = HostConnector(client)
        val networkMonitor = NetworkMonitor(this)
        lifecycleScope.launch {
            val settings = store.settings.first()
            PollScheduler.apply(this@MainActivity, settings.backgroundPoll)
        }
        setContent {
            val settings by store.settings.collectAsState(initial = EdcSettings())
            val launchAction by launchActionState
            PocketApp(
                settings = settings,
                store = store,
                client = client,
                outboxStore = outboxStore,
                outboxProcessor = outboxProcessor,
                hostConnector = hostConnector,
                networkMonitor = networkMonitor,
                launchAction = launchAction,
                onLaunchActionHandled = { launchActionState.value = LaunchAction.NONE },
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchActionState.value = intent.launchAction()
    }
}
