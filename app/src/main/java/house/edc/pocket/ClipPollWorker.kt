package house.edc.pocket

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class ClipPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settingsStore = SettingsStore(context)
        val settings = settingsStore.settings.first()
        if (settings.backgroundPoll == BackgroundPollMode.OFF) return Result.success()
        val base = settings.baseUrl
        if (base.isBlank()) return Result.success()

        val client = EdcClient(context.contentResolver)
        val syncCoordinator = SyncCoordinator(client, SyncCache(context))
        val outcome = runCatching { syncCoordinator.sync(settings) }.getOrNull()
            ?: return Result.retry()

        val latest = outcome.snapshot.latest ?: outcome.snapshot.history.firstOrNull()
        val clipStore = LatestClipStore(context)
        val previousNotified = clipStore.lastNotifiedFingerprint()

        if (latest != null) {
            clipStore.save(latest)
            EdcWidgetUpdater.updateAll(context)
            val fp = clipStore.fingerprint(latest)
            if (previousNotified.isNotEmpty() && fp != previousNotified) {
                clipStore.peek()?.let { cached ->
                    EdcNotifications.showNewClip(context, cached)
                }
            }
            clipStore.setLastNotifiedFingerprint(fp)
        }

        val outbox = OutboxStore(context)
        OutboxProcessor(client, outbox).flush(settings)
        return Result.success()
    }
}
