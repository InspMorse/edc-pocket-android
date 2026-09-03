package house.edc.pocket

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object WearClipPublisher {
    fun publish(context: Context, clip: CachedClip?) {
        runCatching {
            val client = Wearable.getDataClient(context.applicationContext)
            val request = PutDataMapRequest.create("/edc/latest_clip").apply {
                dataMap.putString("text", clip?.text.orEmpty())
                dataMap.putString("from", clip?.from.orEmpty())
                dataMap.putString("ts", clip?.ts.orEmpty())
                dataMap.putLong("updatedAt", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            client.putDataItem(request)
        }
    }
}
