package house.edc.pocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class QrPairActivity : ComponentActivity() {
    private val scanner = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            setResult(RESULT_CANCELED)
            finish()
            return@registerForActivityResult
        }
        val payload = parsePairQr(result.contents)
        if (payload == null) {
            setResult(RESULT_CANCELED)
            finish()
            return@registerForActivityResult
        }
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_PAYLOAD_URL, payload.url)
                .putExtra(EXTRA_PAYLOAD_NAME, payload.name)
                .putExtra(EXTRA_PAYLOAD_PIN, payload.pinSha256),
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan the house dashboard QR code")
            setBeepEnabled(false)
            setOrientationLocked(false)
            setCaptureActivity(QrCaptureActivity::class.java)
        }
        scanner.launch(options)
    }

    companion object {
        const val EXTRA_PAYLOAD_URL = "pair_url"
        const val EXTRA_PAYLOAD_NAME = "pair_name"
        const val EXTRA_PAYLOAD_PIN = "pair_pin"
    }
}
