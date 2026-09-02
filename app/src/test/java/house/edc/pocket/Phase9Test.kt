package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Test

class Phase9Test {
    @Test
    fun parsePinnedSessions_splitsAndTrims() {
        assertEquals(listOf("holiday", "receipts"), parsePinnedSessions(" holiday , receipts , "))
        assertEquals(emptyList<String>(), parsePinnedSessions("  ,  "))
    }

    @Test
    fun formatPinnedSessions_roundTrip() {
        val raw = formatPinnedSessions(listOf("kids", "holiday"))
        assertEquals(listOf("kids", "holiday"), parsePinnedSessions(raw))
    }

    @Test
    fun shareDestination_labels() {
        assertEquals("Ask each time", ShareDestination.ASK.label)
        assertEquals("House clipboard", ShareDestination.CLIP.label)
    }

    @Test
    fun widgetTapAction_openList() {
        assertEquals("Open list", WidgetTapAction.OPEN_LIST.label)
    }

    @Test
    fun outboxRetryStillWorks() {
        assertEquals(5_000L, outboxRetryDelayMs(1))
    }
}
