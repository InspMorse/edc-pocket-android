package house.edc.pocket

enum class BackgroundPollMode(val label: String) {
    OFF("Off"),
    CONSERVATIVE("Conservative"),
    ACTIVE("Active"),
    ;

    val intervalMinutes: Long
        get() = when (this) {
            OFF -> 0L
            CONSERVATIVE -> 60L
            ACTIVE -> 15L
        }
}
