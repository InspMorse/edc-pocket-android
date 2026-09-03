package house.edc.pocket

import android.content.Context
import kotlinx.coroutines.runBlocking

object SurfaceEffects {
    fun afterSnapshot(context: Context, snapshot: HostSnapshot, settings: EdcSettings) {
        val app = context.applicationContext
        WidgetSnapshotStore(app).apply {
            updateFromSnapshot(snapshot)
            syncFromSettings(settings)
        }
        refreshWidgets(app)
    }

    fun afterClipSaved(context: Context, settings: EdcSettings) {
        val app = context.applicationContext
        val clip = LatestClipStore(app).peek()
        WearClipPublisher.publish(app, clip)
        if (settings.persistentClipPreview) {
            PersistentClipService.refresh(app)
        }
        refreshWidgets(app)
    }

    fun applyPersistentPreview(context: Context, enabled: Boolean) {
        PersistentClipService.apply(context, enabled)
    }

    fun syncSessions(context: Context, sessions: List<String>) {
        SessionShortcuts.sync(context, sessions)
    }

    private fun refreshWidgets(context: Context) {
        EdcWidgetUpdater.updateAllBlocking(context)
        runBlocking { EdcLockWidgetUpdater.updateAll(context) }
    }
}
