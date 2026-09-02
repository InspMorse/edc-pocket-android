package house.edc.pocket

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class OpenListTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.tile_open_list)
            subtitle = getString(R.string.tile_open_list_sub)
            updateTile()
        }
    }

    override fun onClick() {
        unlockAndRun {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_OPEN_LIST
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
        }
    }
}
