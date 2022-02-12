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

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.storage.OdysseyDatabaseManager;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.PlaylistParser;
import org.gateshipone.odyssey.utils.PlaylistParserFactory;

import java.lang.ref.WeakReference;
import java.util.List;

public class PlaylistTrackViewModel extends GenericViewModel<TrackModel> {

    /**
     * The playlistModel that contains all information to load the playlist tracks.
     */
    private final PlaylistModel mPlaylistModel;

    private PlaylistTrackViewModel(@NonNull final Application application, final PlaylistModel playlistModel) {
        super(application);

        mPlaylistModel = playlistModel;
    }

    @Override
    void loadData() {
        new PlaylistLoaderTask(this).execute();
    }

    private static class PlaylistLoaderTask extends AsyncTask<Void, Void, List<TrackModel>> {

        private final WeakReference<PlaylistTrackViewModel> mViewModel;

        PlaylistLoaderTask(final PlaylistTrackViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        protected List<TrackModel> doInBackground(Void... voids) {
            final PlaylistTrackViewModel model = mViewModel.get();

            if (model != null) {

                final PlaylistModel playlist = model.mPlaylistModel;
                final Application application = model.getApplication();

                switch (playlist.getPlaylistType()) {
                    case MEDIASTORE:
                        return MusicLibraryHelper.getTracksForPlaylist(playlist.getPlaylistId(), application);
                    case ODYSSEY_LOCAL:
                        return OdysseyDatabaseManager.getInstance(application).getTracksForPlaylist(playlist.getPlaylistId());
                    case FILE:
                        PlaylistParser parser = PlaylistParserFactory.getParser(new FileModel(playlist.getPlaylistPath()));
                        if (parser == null) {
                            return null;
                        }
                        return parser.parseList(model.getApplication());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<TrackModel> result) {
            final PlaylistTrackViewModel model = mViewModel.get();

            if (model != null) {
                model.setData(result);
            }
        }
    }

    public static class PlaylistTrackViewModelFactory implements ViewModelProvider.Factory {

        private final Application mApplication;

        private final PlaylistModel mPlaylistModel;

        public PlaylistTrackViewModelFactory(final Application application, final PlaylistModel playlistModel) {
            mApplication = application;
            mPlaylistModel = playlistModel;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new PlaylistTrackViewModel(mApplication, mPlaylistModel);
        }
    }
}
