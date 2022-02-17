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
import org.gateshipone.odyssey.R
import org.gateshipone.odyssey.models.PlaylistModel
import org.gateshipone.odyssey.playbackservice.storage.OdysseyDatabaseManager
import org.gateshipone.odyssey.utils.MusicLibraryHelper
import java.lang.IllegalArgumentException
import java.util.*

class PlaylistViewModel(
    application: Application,
    private val addHeader: Boolean,
    private val onlyOdysseyPlaylists: Boolean
) : GenericViewModel<PlaylistModel>(application) {

    override fun loadData() {
        viewModelScope.launch {
            val playlists = loadPlaylists()
            setData(playlists)
        }
    }

    private suspend fun loadPlaylists() = withContext(Dispatchers.IO) {
        val application: Application = getApplication()

        val playlists: MutableList<PlaylistModel> = ArrayList()

        if (addHeader) {
            // add a dummy playlist for the choose playlist dialog
            // this playlist represents the action to create a new playlist in the dialog
            playlists.add(
                PlaylistModel(
                    application.getString(R.string.create_new_playlist),
                    -1,
                    PlaylistModel.PLAYLIST_TYPES.CREATE_NEW
                )
            )
        }

        if (!onlyOdysseyPlaylists) {
            // add playlists from the mediastore
            playlists.addAll(MusicLibraryHelper.getAllPlaylists(application))
        }

        // add playlists from odyssey local storage

        // add playlists from odyssey local storage
        playlists.addAll(OdysseyDatabaseManager.getInstance(application).playlists)

        // sort the playlist

        // sort the playlist
        playlists.sortWith(Comparator { p1: PlaylistModel, p2: PlaylistModel ->
            // make sure that the place holder for a new playlist is always at the top
            if (p1.playlistType == PlaylistModel.PLAYLIST_TYPES.CREATE_NEW) {
                return@Comparator -1
            }
            if (p2.playlistType == PlaylistModel.PLAYLIST_TYPES.CREATE_NEW) {
                return@Comparator 1
            }
            p1.playlistName.compareTo(p2.playlistName, ignoreCase = true)
        })

        playlists
    }

    class PlaylistViewModelFactory(
        private val application: Application,
        private val addHeader: Boolean,
        private val onlyOdysseyPlaylists: Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
                return PlaylistViewModel(application, addHeader, onlyOdysseyPlaylists) as T
            }
            throw IllegalArgumentException("unknown viewmodel class")
        }

    }
}
