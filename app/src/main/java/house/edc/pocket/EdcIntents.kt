package house.edc.pocket

object EdcIntents {
    const val ACTION_COPY_CLIP = "house.edc.pocket.action.COPY_CLIP"
    const val ACTION_OPEN_APP = "house.edc.pocket.action.OPEN_APP"
    const val NOTIFICATION_CHANNEL = "edc_clips"
    const val NOTIFICATION_CHANNEL_PERSISTENT = "edc_persistent"
    const val NOTIFICATION_ID_CLIP = 7101
    const val NOTIFICATION_ID_PERSISTENT = 7102

    const val EXTRA_CLIP_FILTER = "house.edc.pocket.extra.CLIP_FILTER"
    const val EXTRA_SESSION = "house.edc.pocket.extra.SESSION"
    const val EXTRA_SKIP_SESSION_PROMPT = "house.edc.pocket.extra.SKIP_SESSION_PROMPT"
    const val EXTRA_SHARE_DESTINATION = "house.edc.pocket.extra.SHARE_DESTINATION"

    const val ACTION_AUTOMATION_COPY = "house.edc.pocket.action.AUTOMATION_COPY"
    const val ACTION_AUTOMATION_SEND_CLIP = "house.edc.pocket.action.AUTOMATION_SEND_CLIP"
    const val ACTION_AUTOMATION_SEND_LIST = "house.edc.pocket.action.AUTOMATION_SEND_LIST"
    const val ACTION_AUTOMATION_OPEN_LIST = "house.edc.pocket.action.AUTOMATION_OPEN_LIST"
    const val ACTION_AUTOMATION_OPEN_SEND = "house.edc.pocket.action.AUTOMATION_OPEN_SEND"

    const val NFC_SCHEME = "edc"
}
