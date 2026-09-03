package house.edc.pocket

enum class ShareDestination(val label: String) {
    ASK("Ask each time"),
    CLIP("House clipboard"),
    LIST("To-do list"),
}

enum class WidgetTapAction(val label: String) {
    OPEN_APP("Open app"),
    OPEN_LIST("Open list"),
    COPY_CLIP("Copy latest"),
    OPEN_CLIP("Open clip tab"),
}

enum class NfcAction(val label: String) {
    COPY_CLIP("Copy latest clip"),
    OPEN_APP("Open EDC pocket"),
    OPEN_LIST("Open list"),
}
