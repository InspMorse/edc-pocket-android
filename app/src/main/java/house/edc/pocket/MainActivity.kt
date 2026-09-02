package house.edc.pocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val launchActionState = mutableStateOf(LaunchAction.NONE)
    private val launchTextState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EdcNotifications.ensureChannel(this)
        applyLaunchIntent(intent)
        val store = SettingsStore(this)
        val outboxStore = OutboxStore(this)
        val client = EdcClient(contentResolver)
        val syncCache = SyncCache(this)
        val syncCoordinator = SyncCoordinator(client, syncCache)
        val outboxProcessor = OutboxProcessor(client, outboxStore)
        val hostConnector = HostConnector(client)
        val networkMonitor = NetworkMonitor(this)
        val connectionDoctor = ConnectionDoctor(client)
        lifecycleScope.launch {
            val settings = store.settings.first()
            PollScheduler.apply(this@MainActivity, settings.backgroundPoll)
            intent.launchClipFilter().takeIf { it.isNotBlank() }?.let { store.setClipFilter(it) }
        }
        setContent {
            val settings by store.settings.collectAsState(initial = EdcSettings())
            val launchAction by launchActionState
            val launchText by launchTextState
            LaunchedEffect(settings.persistentClipPreview) {
                SurfaceEffects.applyPersistentPreview(this@MainActivity, settings.persistentClipPreview)
            }
            LaunchedEffect(settings.pinnedSessions) {
                SurfaceEffects.syncSessions(this@MainActivity, settings.pinnedSessions)
            }
            LaunchedEffect(
                settings.widgetShowTodoCount,
                settings.widgetTapAction,
                settings.clipFilter,
            ) {
                WidgetSnapshotStore(this@MainActivity).syncFromSettings(settings)
                EdcWidgetUpdater.updateAllBlocking(this@MainActivity)
            }
            PocketApp(
                settings = settings,
                store = store,
                client = client,
                syncCoordinator = syncCoordinator,
                outboxStore = outboxStore,
                outboxProcessor = outboxProcessor,
                hostConnector = hostConnector,
                networkMonitor = networkMonitor,
                connectionDoctor = connectionDoctor,
                launchAction = launchAction,
                launchText = launchText,
                onLaunchActionHandled = {
                    launchActionState.value = LaunchAction.NONE
                    launchTextState.value = ""
                },
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchIntent(intent)
    }

    private fun applyLaunchIntent(intent: android.content.Intent) {
        launchActionState.value = intent.launchAction()
        launchTextState.value = intent.launchText()
    }
}
