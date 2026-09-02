package house.edc.pocket

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object EdcNotifications {
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = android.app.NotificationChannel(
            EdcIntents.NOTIFICATION_CHANNEL,
            "House clipboard",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "New items on the house clipboard"
        }
        manager.createNotificationChannel(channel)
    }

    fun showNewClip(context: Context, clip: CachedClip) {
        ensureChannel(context)
        val appContext = context.applicationContext
        val copyIntent = Intent(appContext, CopyClipReceiver::class.java).apply {
            action = EdcIntents.ACTION_COPY_CLIP
        }
        val copyPending = PendingIntent.getBroadcast(
            appContext,
            0,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            appContext,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val from = clip.from.ifBlank { "EDC" }
        val preview = clip.text.take(120)
        val notification = NotificationCompat.Builder(appContext, EdcIntents.NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_edc)
            .setContentTitle("New house clip · $from")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(clip.text))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(0, "Copy", copyPending)
            .addAction(0, "Open", openPending)
            .build()
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.notify(EdcIntents.NOTIFICATION_ID_CLIP, notification)
    }
}
