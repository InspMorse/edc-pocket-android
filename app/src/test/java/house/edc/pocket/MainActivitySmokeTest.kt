package house.edc.pocket

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MainActivitySmokeTest {
    @Test
    fun mainActivityLaunches() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
    }
}
