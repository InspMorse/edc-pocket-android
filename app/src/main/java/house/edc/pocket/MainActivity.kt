package house.edc.pocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val launchActionState = mutableStateOf(LaunchAction.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchActionState.value = intent.launchAction()
        val store = SettingsStore(this)
        val client = EdcClient(contentResolver)
        setContent {
            val settings by store.settings.collectAsState(initial = EdcSettings())
            val launchAction by launchActionState
            PocketApp(
                settings = settings,
                store = store,
                client = client,
                launchAction = launchAction,
                onLaunchActionHandled = { launchActionState.value = LaunchAction.NONE },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchActionState.value = intent.launchAction()
    }
}
