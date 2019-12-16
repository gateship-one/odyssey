/*
 * Copyright (C) 2020 Team Gateship-One
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

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.gateshipone.odyssey.database.MusicDatabaseFactory;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.lang.ref.WeakReference;
import java.util.List;

public class TrackViewModel extends GenericViewModel<TrackModel> {

    /**
     * The album key if tracks of a specific album should be loaded.
     */
    private final AlbumModel mAlbum;

    /**
     * The playlist id if tracks of a specific playlist should be loaded.
     */
    private final PlaylistModel mPlaylist;

    private TrackViewModel(@NonNull final Application application, final AlbumModel album, final PlaylistModel playlist) {
        super(application);

        mAlbum = album;
        mPlaylist = playlist;
    }

    @Override
    void loadData() {
        new TrackLoaderTask(this).execute();
    }

    private static class TrackLoaderTask extends AsyncTask<Void, Void, List<TrackModel>> {

        private final WeakReference<TrackViewModel> mViewModel;

        TrackLoaderTask(final TrackViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        protected List<TrackModel> doInBackground(Void... voids) {
            final TrackViewModel model = mViewModel.get();

            if (model != null) {
                final Application application = model.getApplication();

                if (model.mPlaylist != null) {
                    // load playlist tracks
                    return MusicDatabaseFactory.getDatabase(application).getTracksForPlaylist(model.mPlaylist, application);
                } else {
                    if (model.mAlbum == null) {
                        // load all tracks
                        return MusicDatabaseFactory.getDatabase(application).getAllTracks(null, application);
                    } else {
                        // load album tracks
                        return MusicDatabaseFactory.getDatabase(application).getTracksForAlbum(model.mAlbum, application);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<TrackModel> result) {
            final TrackViewModel model = mViewModel.get();

            if (model != null) {
                model.setData(result);
            }
        }
    }


    public static class TrackViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        private final AlbumModel mAlbum;
        private final PlaylistModel mPlaylist;

        private TrackViewModelFactory(final Application application, final AlbumModel album, final PlaylistModel playlist) {
            mApplication = application;
            mAlbum = album;
            mPlaylist = playlist;
        }

        public TrackViewModelFactory(final Application application) {
            this(application, null, null);
        }

        public TrackViewModelFactory(final Application application, final AlbumModel album) {
            this(application, album, null);
        }

        public TrackViewModelFactory(final Application application, final PlaylistModel playlist) {
            this(application, null, playlist);
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new TrackViewModel(mApplication, mAlbum, mPlaylist);
        }
    }
}
