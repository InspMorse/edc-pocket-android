package house.edc.pocket

import android.content.Intent

enum class LaunchAction {
    NONE,
    COPY_LATEST,
    OPEN_SEND,
    OPEN_LIST,
}

fun Intent.launchAction(): LaunchAction = when (action) {
    ACTION_COPY_LATEST -> LaunchAction.COPY_LATEST
    ACTION_OPEN_SEND -> LaunchAction.OPEN_SEND
    ACTION_OPEN_LIST -> LaunchAction.OPEN_LIST
    else -> LaunchAction.NONE
}

const val ACTION_COPY_LATEST = "house.edc.pocket.action.COPY_LATEST"
const val ACTION_OPEN_SEND = "house.edc.pocket.action.OPEN_SEND"
const val ACTION_OPEN_LIST = "house.edc.pocket.action.OPEN_LIST"
