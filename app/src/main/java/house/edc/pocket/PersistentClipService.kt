package house.edc.pocket

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PersistentClipService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                EdcNotifications.ensurePersistentChannel(this)
                startForeground(EdcIntents.NOTIFICATION_ID_PERSISTENT, buildNotification())
                return START_STICKY
            }
        }
    }

    private fun buildNotification(): Notification {
        val clip = LatestClipStore(this).peek()
        val preview = clip?.text?.take(80) ?: "Connected — waiting for house clip"
        val from = clip?.from?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        val copyIntent = Intent(this, CopyClipReceiver::class.java).apply {
            action = EdcIntents.ACTION_COPY_CLIP
        }
        val copyPending = PendingIntent.getBroadcast(
            this,
            2,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            3,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, EdcIntents.NOTIFICATION_CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_stat_edc)
            .setContentTitle("EDC pocket$from")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(clip?.text ?: preview))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, "Copy", copyPending)
            .addAction(0, "Open", openPending)
            .build()
    }

    companion object {
        private const val ACTION_STOP = "house.edc.pocket.action.STOP_PERSISTENT"

        fun apply(context: Context, enabled: Boolean) {
            val app = context.applicationContext
            if (enabled) {
                val intent = Intent(app, PersistentClipService::class.java)
                app.startForegroundService(intent)
            } else {
                app.startService(
                    Intent(app, PersistentClipService::class.java).apply { action = ACTION_STOP },
                )
            }
        }

        fun refresh(context: Context) {
            val app = context.applicationContext
            app.startService(Intent(app, PersistentClipService::class.java))
        }
    }
}
