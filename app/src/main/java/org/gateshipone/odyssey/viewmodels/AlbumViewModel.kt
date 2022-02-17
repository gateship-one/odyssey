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
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.gateshipone.odyssey.R
import org.gateshipone.odyssey.models.AlbumModel
import org.gateshipone.odyssey.utils.MusicLibraryHelper

class AlbumViewModel(
    application: Application,
    private val artistID: Long,
    private val loadRecent: Boolean,
) : GenericViewModel<AlbumModel>(application) {

    override fun loadData() {
        viewModelScope.launch {
            val albums = loadAlbums()
            setData(albums)
        }
    }

    private suspend fun loadAlbums() = withContext(Dispatchers.IO) {
        val application: Application = getApplication()

        return@withContext if (artistID == -1L) {
            if (loadRecent) {
                // load recent albums
                MusicLibraryHelper.getRecentAlbums(application)
            } else {
                // load all albums
                MusicLibraryHelper.getAllAlbums(application)
            }
        } else {
            // load all albums from the given artist

            // Read order preference
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(application)
            val orderKey = sharedPref.getString(
                application.getString(R.string.pref_album_sort_order_key),
                application.getString(R.string.pref_artist_albums_sort_default)
            )
            MusicLibraryHelper.getAllAlbumsForArtist(artistID, orderKey, application)
        }
    }

    class AlbumViewModelFactory(
        private val application: Application,
        private val artistID: Long,
        private val loadRecent: Boolean,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
                return AlbumViewModel(application, artistID, loadRecent) as T
            }
            throw IllegalArgumentException("unknown viewmodel class")
        }
    }
}
