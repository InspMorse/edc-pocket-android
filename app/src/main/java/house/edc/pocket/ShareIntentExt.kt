package house.edc.pocket

import android.content.Intent
import android.net.Uri
import android.os.Build

internal fun Intent.imageUris(): List<Uri> {
    if (action == Intent.ACTION_SEND_MULTIPLE) {
        val streams = if (Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
        return streams.orEmpty()
    }
    return parcelUri()?.let { listOf(it) }.orEmpty()
}

internal fun Intent.parcelUri(): Uri? {
    val extra = extras ?: return null
    val stream = if (Build.VERSION.SDK_INT >= 33) {
        extra.getParcelable(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        extra.getParcelable(Intent.EXTRA_STREAM) as? Uri
    }
    return stream ?: data
}
