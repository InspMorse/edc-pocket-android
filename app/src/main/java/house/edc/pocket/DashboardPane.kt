package house.edc.pocket

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DashboardPane(
    url: String,
    identity: String,
) {
    if (url.isBlank()) return
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = false
                }
                val join = if (url.contains("?")) "&" else "?"
                loadUrl("$url${join}as=${java.net.URLEncoder.encode(identity, "UTF-8")}")
            }
        },
        update = { view ->
            val join = if (url.contains("?")) "&" else "?"
            val target = "$url${join}as=${java.net.URLEncoder.encode(identity, "UTF-8")}"
            if (view.url != target) view.loadUrl(target)
        },
    )
}
