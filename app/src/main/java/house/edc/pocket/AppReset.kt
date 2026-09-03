package house.edc.pocket

import android.content.Context
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first

object AppReset {
    suspend fun resetAll(
        context: Context,
        store: SettingsStore,
        outbox: OutboxStore,
        auditLog: AuditLogStore,
        telemetry: TelemetryStore,
        syncCache: SyncCache,
        pinStore: PinStore,
        todoExtras: TodoExtrasStore,
        settings: EdcSettings,
    ) {
        val app = context.applicationContext
        WorkManager.getInstance(app).cancelAllWork()
        PollScheduler.apply(app, BackgroundPollMode.OFF)
        PersistentClipService.apply(app, false)
        outbox.clear()
        auditLog.clear()
        telemetry.clear()
        syncCache.clearAll()
        pinStore.clear()
        todoExtras.clear()
        store.resetToDefaults()
        WidgetSnapshotStore(app).clear()
        EdcWidgetUpdater.updateAllBlocking(app)
        auditLog.append(
            kind = AuditKind.RESET,
            identity = settings.effectiveIdentity,
            detail = "Local app data cleared",
            success = true,
        )
    }
}
