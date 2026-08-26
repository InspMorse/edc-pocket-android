package house.edc.pocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SettingsStore(this)
        val client = EdcClient(contentResolver)
        setContent {
            val settings by store.settings.collectAsState(initial = EdcSettings())
            PocketApp(settings = settings, store = store, client = client)
        }
    }
}
