package house.edc.pocket

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DropItemTest {
    @Test
    fun imageExtensions() {
        assertTrue(DropItem("1", "photo.jpg", "", "", 0).isImage())
        assertTrue(DropItem("1", "pics/a.png", "", "", 0).isImage())
        assertFalse(DropItem("1", "notes.txt", "", "", 0).isImage())
    }
}
