package house.edc.pocket

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val LockBg = ColorProvider(0xFF07080A.toInt())
private val LockCyan = ColorProvider(0xFF22D3EE.toInt())
private val LockInk = ColorProvider(0xFFE2E8F0.toInt())

@Composable
fun EdcLockWidgetContent(context: Context) {
    val clip = LatestClipStore(context).peek()
    val copyClip = actionSendBroadcast(
        Intent(context, CopyClipReceiver::class.java).apply {
            action = EdcIntents.ACTION_COPY_CLIP
        },
    )
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(LockBg)
            .padding(8.dp)
            .clickable(copyClip),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EDC",
            style = TextStyle(color = LockCyan, fontSize = 11.sp),
        )
        Text(
            text = clip?.text?.ifBlank { "Tap to copy house clip" } ?: "Tap to copy house clip",
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            style = TextStyle(color = LockInk, fontSize = 13.sp),
            maxLines = 2,
        )
    }
}
