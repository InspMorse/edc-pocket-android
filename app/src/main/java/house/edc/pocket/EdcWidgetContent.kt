package house.edc.pocket

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val Bg = ColorProvider(0xFF07080A.toInt())
private val Cyan = ColorProvider(0xFF22D3EE.toInt())
private val Ink = ColorProvider(0xFFE2E8F0.toInt())
private val Muted = ColorProvider(0xFF94A3B8.toInt())

@Composable
fun EdcWidgetContent(context: Context) {
    val clip = LatestClipStore(context).peek()
    val openApp = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
    )
    val copyClip = actionSendBroadcast(
        Intent(context, CopyClipReceiver::class.java).apply {
            action = EdcIntents.ACTION_COPY_CLIP
        },
    )
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Bg)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "EDC pocket",
            style = TextStyle(color = Cyan, fontSize = 12.sp),
        )
        Text(
            text = clip?.text?.ifBlank { "Nothing on the house clipboard." }
                ?: "Nothing on the house clipboard.",
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 8.dp),
            style = TextStyle(color = Ink, fontSize = 14.sp),
            maxLines = 4,
        )
        val meta = clip?.from?.takeIf { it.isNotBlank() }?.let { "From $it" } ?: "Tap Open"
        Text(
            text = meta,
            style = TextStyle(color = Muted, fontSize = 11.sp),
        )
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(
                text = "Copy",
                modifier = GlanceModifier
                    .padding(end = 16.dp)
                    .clickable(copyClip),
                style = TextStyle(color = Cyan, fontSize = 13.sp),
            )
            Text(
                text = "Open",
                modifier = GlanceModifier.clickable(openApp),
                style = TextStyle(color = Cyan, fontSize = 13.sp),
            )
        }
    }
}
