package house.edc.pocket

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent

class EdcLockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            EdcLockWidgetContent(context)
        }
    }
}

object EdcLockWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context.applicationContext)
        manager.getGlanceIds(EdcLockWidget::class.java).forEach { glanceId ->
            EdcLockWidget().update(context.applicationContext, glanceId)
        }
    }
}
