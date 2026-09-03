package house.edc.pocket

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PollScheduler {
    private const val WORK_NAME = "edc_clip_poll"

    fun apply(context: Context, mode: BackgroundPollMode) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(WORK_NAME)
        if (mode == BackgroundPollMode.OFF) return
        val interval = mode.intervalMinutes.coerceAtLeast(15L)
        val request = PeriodicWorkRequestBuilder<ClipPollWorker>(interval, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
