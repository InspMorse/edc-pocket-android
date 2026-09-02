package house.edc.pocket

import android.content.Intent

enum class LaunchAction {
    NONE,
    COPY_LATEST,
    OPEN_SEND,
    OPEN_SEND_CAMERA,
    OPEN_LIST,
    OPEN_CLIP,
    SEND_TO_CLIP,
    SEND_TO_LIST,
}

fun Intent.launchAction(): LaunchAction = when (action) {
    ACTION_COPY_LATEST -> LaunchAction.COPY_LATEST
    ACTION_OPEN_SEND -> LaunchAction.OPEN_SEND
    ACTION_OPEN_SEND_CAMERA -> LaunchAction.OPEN_SEND_CAMERA
    ACTION_OPEN_LIST -> LaunchAction.OPEN_LIST
    ACTION_OPEN_CLIP -> LaunchAction.OPEN_CLIP
    ACTION_SEND_TO_CLIP -> LaunchAction.SEND_TO_CLIP
    ACTION_SEND_TO_LIST -> LaunchAction.SEND_TO_LIST
    EdcIntents.ACTION_AUTOMATION_COPY -> LaunchAction.COPY_LATEST
    EdcIntents.ACTION_AUTOMATION_OPEN_LIST -> LaunchAction.OPEN_LIST
    EdcIntents.ACTION_AUTOMATION_OPEN_SEND -> LaunchAction.OPEN_SEND
    EdcIntents.ACTION_AUTOMATION_SEND_CLIP -> LaunchAction.SEND_TO_CLIP
    EdcIntents.ACTION_AUTOMATION_SEND_LIST -> LaunchAction.SEND_TO_LIST
    else -> LaunchAction.NONE
}

fun Intent.launchText(): String =
    getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()

fun Intent.launchClipFilter(): String =
    getStringExtra(EdcIntents.EXTRA_CLIP_FILTER)?.trim().orEmpty()

const val ACTION_COPY_LATEST = "house.edc.pocket.action.COPY_LATEST"
const val ACTION_OPEN_SEND = "house.edc.pocket.action.OPEN_SEND"
const val ACTION_OPEN_SEND_CAMERA = "house.edc.pocket.action.OPEN_SEND_CAMERA"
const val ACTION_OPEN_LIST = "house.edc.pocket.action.OPEN_LIST"
const val ACTION_OPEN_CLIP = "house.edc.pocket.action.OPEN_CLIP"
const val ACTION_SEND_TO_CLIP = "house.edc.pocket.action.SEND_TO_CLIP"
const val ACTION_SEND_TO_LIST = "house.edc.pocket.action.SEND_TO_LIST"
