package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Test

class OutboxModelsTest {
    @Test
    fun roundTripJson() {
        val item = OutboxItem(
            id = "abc",
            kind = OutboxKind.LIST,
            text = "milk",
        )
        val json = OutboxItem.listToJson(listOf(item))
        val parsed = OutboxItem.listFromJson(json).single()
        assertEquals(item.id, parsed.id)
        assertEquals(OutboxKind.LIST, parsed.kind)
        assertEquals("milk", parsed.text)
    }
}
