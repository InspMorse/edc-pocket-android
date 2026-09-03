package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundPollModeTest {
    @Test
    fun intervals() {
        assertEquals(0L, BackgroundPollMode.OFF.intervalMinutes)
        assertEquals(60L, BackgroundPollMode.CONSERVATIVE.intervalMinutes)
        assertEquals(15L, BackgroundPollMode.ACTIVE.intervalMinutes)
    }
}
