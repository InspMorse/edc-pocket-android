package house.edc.pocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val launchActionState = mutableStateOf(LaunchAction.NONE)
    private val launchTextState = mutableStateOf("")

    private val qrPairLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val url = data.getStringExtra(QrPairActivity.EXTRA_PAYLOAD_URL).orEmpty()
        if (url.isBlank()) return@registerForActivityResult
        lifecycleScope.launch {
            settingsStore().applyPairPayload(
                PairQrPayload(
                    url = url,
                    name = data.getStringExtra(QrPairActivity.EXTRA_PAYLOAD_NAME).orEmpty(),
                    pinSha256 = data.getStringExtra(QrPairActivity.EXTRA_PAYLOAD_PIN).orEmpty(),
                ),
            )
        }
    }

    private var cachedStore: SettingsStore? = null
    private fun settingsStore(): SettingsStore {
        cachedStore?.let { return it }
        return SettingsStore(this).also { cachedStore = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        EdcNotifications.ensureChannel(this)
        applyLaunchIntent(intent)
        val store = settingsStore()
        val outboxStore = OutboxStore(this)
        val client = EdcClient(contentResolver)
        val syncCache = SyncCache(this)
        val syncCoordinator = SyncCoordinator(client, syncCache)
        val outboxProcessor = OutboxProcessor(client, outboxStore)
        val hostConnector = HostConnector(client)
        val networkMonitor = NetworkMonitor(this)
        val connectionDoctor = ConnectionDoctor(client)
        val homeHintMonitor = HomeHintMonitor(this)
        lifecycleScope.launch {
            val settings = store.settings.first()
            applyTls(client, settings)
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
            LaunchedEffect(settings.baseUrl, settings.useHttps, settings.tlsPinSha256) {
                applyTls(client, settings)
            }
            BiometricGate(enabled = settings.biometricLock) {
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
                    homeHintMonitor = homeHintMonitor,
                    launchAction = launchAction,
                    launchText = launchText,
                    onLaunchActionHandled = {
                        launchActionState.value = LaunchAction.NONE
                        launchTextState.value = ""
                    },
                    onScanQrPair = {
                        qrPairLauncher.launch(Intent(this@MainActivity, QrPairActivity::class.java))
                    },
                )
            }
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

    private fun applyTls(client: EdcClient, settings: EdcSettings) {
        val base = settings.baseUrl
        val pin = settings.tlsPinSha256
        if (!base.startsWith("https://", ignoreCase = true) || pin.isBlank()) {
            client.updateTlsConfig(TlsPinConfig())
            return
        }
        client.updateTlsConfig(
            TlsPinConfig(
                hostPattern = TlsPinning.hostPattern(base),
                pinSha256 = pin,
            ),
        )
    }
}
