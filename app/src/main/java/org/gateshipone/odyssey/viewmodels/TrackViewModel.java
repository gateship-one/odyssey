/*
 * Copyright (C) 2018 Team Team Gateship-One
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

package org.gateshipone.odyssey.viewmodels;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.util.List;

public class TrackViewModel extends GenericViewModel<TrackModel> {

    /**
     * The album key if tracks of a specific album should be loaded.
     */
    private final String mAlbumKey;

    /**
     * The playlist id if tracks of a specific playlist should be loaded.
     */
    private final long mPlaylistID;

    private TrackViewModel(@NonNull final Application application, final String albumKey, final long playlistID) {
        super(application);

        mAlbumKey = albumKey;
        mPlaylistID = playlistID;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    void loadData() {
        new AsyncTask<Void, Void, List<TrackModel>>() {

            @Override
            protected List<TrackModel> doInBackground(Void... voids) {
                final Application application = getApplication();

                if (mPlaylistID != -1) {
                    // load playlist tracks
                    return MusicLibraryHelper.getTracksForPlaylist(mPlaylistID, application);
                } else {
                    if (mAlbumKey.isEmpty()) {
                        // load all tracks
                        return MusicLibraryHelper.getAllTracks(null, application);
                    } else {
                        // load album tracks
                        return MusicLibraryHelper.getTracksForAlbum(mAlbumKey, application);
                    }
                }
            }

            @Override
            protected void onPostExecute(List<TrackModel> result) {
                setData(result);
            }
        }.execute();
    }

    public static class TrackViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        private final String mAlbumKey;

        private final long mPlaylistID;

        private TrackViewModelFactory(final Application application, final String albumKey, final long playlistID) {
            mApplication = application;
            mAlbumKey = albumKey;
            mPlaylistID = playlistID;
        }

        public TrackViewModelFactory(final Application application) {
            this(application, "", -1);
        }

        public TrackViewModelFactory(final Application application, final String albumKey) {
            this(application, albumKey, -1);
        }

        public TrackViewModelFactory(final Application application, final long playlistID) {
            this(application, "", playlistID);
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new TrackViewModel(mApplication, mAlbumKey, mPlaylistID);
        }
    }
}
