/*
 * Copyright (C) 2022 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.gateshipone.odyssey.models.FileModel
import org.gateshipone.odyssey.models.PlaylistModel
import org.gateshipone.odyssey.models.PlaylistModel.PLAYLIST_TYPES
import org.gateshipone.odyssey.models.TrackModel
import org.gateshipone.odyssey.playbackservice.storage.OdysseyDatabaseManager
import org.gateshipone.odyssey.utils.MusicLibraryHelper
import org.gateshipone.odyssey.utils.PlaylistParserFactory
import java.lang.IllegalArgumentException

class PlaylistTrackViewModel(
    application: Application,
    private val playlistModel: PlaylistModel
) : GenericViewModel<TrackModel>(application) {

    override fun loadData() {
        viewModelScope.launch {
            val playlistTracks = loadPlaylistTracks()
            setData(playlistTracks)
        }
    }

    private suspend fun loadPlaylistTracks() = withContext(Dispatchers.IO) {
        val application: Application = getApplication()

        when (playlistModel.playlistType) {
            PLAYLIST_TYPES.MEDIASTORE -> MusicLibraryHelper.getTracksForPlaylist(
                playlistModel.playlistId,
                application
            )
            PLAYLIST_TYPES.ODYSSEY_LOCAL -> OdysseyDatabaseManager.getInstance(application)
                .getTracksForPlaylist(playlistModel.playlistId)
            PLAYLIST_TYPES.FILE -> {
                val parser = PlaylistParserFactory.getParser(FileModel(playlistModel.playlistPath))
                parser?.parseList(application)
            }
            else -> {
                null
            }
        }
    }

    class PlaylistTrackViewModelFactory(
        private val application: Application,
        private val playlistModel: PlaylistModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlaylistTrackViewModel::class.java)) {
                return PlaylistTrackViewModel(application, playlistModel) as T
            }
            throw IllegalArgumentException("unknown viewmodel class")
        }

    }
}
