package house.edc.pocket

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent

class EdcWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            EdcWidgetContent(context)
        }
    }
}

object EdcWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context.applicationContext)
        manager.getGlanceIds(EdcWidget::class.java).forEach { glanceId ->
            EdcWidget().update(context.applicationContext, glanceId)
        }
    }

    fun updateAllBlocking(context: Context) {
        kotlinx.coroutines.runBlocking {
            updateAll(context)
        }
    }
}
