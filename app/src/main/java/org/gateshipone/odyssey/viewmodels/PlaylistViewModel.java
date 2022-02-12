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

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.playbackservice.storage.OdysseyDatabaseManager;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistViewModel extends GenericViewModel<PlaylistModel> {

    /**
     * Flag if a header element should be inserted.
     */
    private final boolean mAddHeader;

    /**
     * Flag if only playlists in the odyssey db should be loaded.
     * Needed for saving/overriding playlists.
     */
    private final boolean mOnlyOdysseyPlaylists;

    private PlaylistViewModel(@NonNull final Application application, final boolean addHeader, final boolean onlyOdysseyPlaylists) {
        super(application);

        mAddHeader = addHeader;
        mOnlyOdysseyPlaylists = onlyOdysseyPlaylists;
    }

    @Override
    void loadData() {
        new PlaylistLoaderTask(this).execute();
    }

    private static class PlaylistLoaderTask extends AsyncTask<Void, Void, List<PlaylistModel>> {

        private final WeakReference<PlaylistViewModel> mViewModel;

        PlaylistLoaderTask(final PlaylistViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        protected List<PlaylistModel> doInBackground(Void... voids) {
            final PlaylistViewModel model = mViewModel.get();

            if (model != null) {
                final Application application = model.getApplication();

                List<PlaylistModel> playlists = new ArrayList<>();

                if (model.mAddHeader) {
                    // add a dummy playlist for the choose playlist dialog
                    // this playlist represents the action to create a new playlist in the dialog
                    playlists.add(new PlaylistModel(application.getString(R.string.create_new_playlist), -1, PlaylistModel.PLAYLIST_TYPES.CREATE_NEW));
                }

                if (!model.mOnlyOdysseyPlaylists) {
                    // add playlists from the mediastore
                    playlists.addAll(MusicLibraryHelper.getAllPlaylists(application));
                }

                // add playlists from odyssey local storage
                playlists.addAll(OdysseyDatabaseManager.getInstance(application).getPlaylists());

                // sort the playlist
                Collections.sort(playlists, (p1, p2) -> {
                    // make sure that the place holder for a new playlist is always at the top
                    if (p1.getPlaylistType() == PlaylistModel.PLAYLIST_TYPES.CREATE_NEW) {
                        return -1;
                    }

                    if (p2.getPlaylistType() == PlaylistModel.PLAYLIST_TYPES.CREATE_NEW) {
                        return 1;
                    }

                    return p1.getPlaylistName().compareToIgnoreCase(p2.getPlaylistName());
                });

                return playlists;
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<PlaylistModel> result) {
            final PlaylistViewModel model = mViewModel.get();

            if (model != null) {
                model.setData(result);
            }
        }
    }

    public static class PlaylistViewModelFactory implements ViewModelProvider.Factory {

        private final Application mApplication;

        private final boolean mAddHeader;

        private final boolean mOnlyOdysseyPlaylists;

        public PlaylistViewModelFactory(final Application application, final boolean addHeader, final boolean onlyOdysseyPlaylists) {
            mApplication = application;
            mAddHeader = addHeader;
            mOnlyOdysseyPlaylists = onlyOdysseyPlaylists;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new PlaylistViewModel(mApplication, mAddHeader, mOnlyOdysseyPlaylists);
        }
    }
}
