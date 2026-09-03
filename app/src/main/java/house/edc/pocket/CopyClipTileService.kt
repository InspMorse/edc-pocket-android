package house.edc.pocket

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.runBlocking

class CopyClipTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        unlockAndRun {
            val text = runBlocking { ClipActions.copyHouseClipboard(this@CopyClipTileService) }
            if (text.isNullOrBlank()) {
                ClipActions.toast(this, "Could not copy clip")
            } else {
                ClipActions.toast(this, "Copied house clip")
            }
            refreshTile()
        }
    }

    private fun refreshTile() {
        val clip = LatestClipStore(this).peek()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "EDC clip"
            subtitle = clip?.text?.lineSequence()?.firstOrNull()?.take(40) ?: "Tap to copy"
            updateTile()
        }
    }
}
