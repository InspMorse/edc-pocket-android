package house.edc.pocket

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NfcDispatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            handleIntent(intent)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lifecycleScope.launch {
            handleIntent(intent)
            finish()
        }
    }

    private suspend fun handleIntent(intent: Intent) {
        val uri = readTagUri(intent) ?: run {
            applyDefaultAction()
            return
        }
        when (uri.host?.lowercase()) {
            "copy" -> {
                val text = ClipActions.copyHouseClipboard(this)
                ClipActions.toast(
                    this,
                    if (text.isNullOrBlank()) "Could not copy clip" else "Copied house clip",
                )
            }
            "list" -> startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_OPEN_LIST
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
            "open" -> startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
            "send" -> {
                val text = uri.getQueryParameter("text").orEmpty()
                if (text.isBlank()) {
                    ClipActions.toast(this, "No text on NFC tag")
                } else {
                    AutomationActions.handle(
                        this,
                        Intent(EdcIntents.ACTION_AUTOMATION_SEND_CLIP).apply {
                            putExtra(Intent.EXTRA_TEXT, text)
                        },
                    )
                }
            }
            else -> applyDefaultAction()
        }
    }

    private suspend fun applyDefaultAction() {
        val settings = SettingsStore(this).settings.first()
        when (settings.nfcAction) {
            NfcAction.COPY_CLIP -> {
                val text = ClipActions.copyHouseClipboard(this)
                ClipActions.toast(
                    this,
                    if (text.isNullOrBlank()) "Could not copy clip" else "Copied house clip",
                )
            }
            NfcAction.OPEN_LIST -> startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_OPEN_LIST
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
            NfcAction.OPEN_APP -> startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        }
    }

    private fun readTagUri(intent: Intent): android.net.Uri? {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED != intent.action &&
            NfcAdapter.ACTION_TAG_DISCOVERED != intent.action
        ) {
            return intent.data
        }
        val raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return intent.data
        for (message in raw) {
            val ndef = message as? NdefMessage ?: continue
            for (record in ndef.records) {
                if (record.toUri() != null) return record.toUri()
            }
        }
        return intent.data
    }
}
