/*
 * Copyright (C) 2019 Team Gateship-One
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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class AlbumViewModel extends GenericViewModel<AlbumModel> {

    /**
     * The artist id if albums of a specific artist should be loaded.
     */
    private final long mArtistID;

    /**
     * Load only the recent albums.
     */
    private final boolean mLoadRecent;

    private AlbumViewModel(@NonNull final Application application, final long artistId, final boolean loadRecent) {
        super(application);

        mArtistID = artistId;
        mLoadRecent = loadRecent;
    }

    @Override
    void loadData() {
        new AlbumLoaderTask(this).execute();
    }

    private static class AlbumLoaderTask extends AsyncTask<Void, Void, List<AlbumModel>> {

        private final WeakReference<AlbumViewModel> mViewModel;

        AlbumLoaderTask(final AlbumViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        protected List<AlbumModel> doInBackground(Void... voids) {
            final AlbumViewModel model = mViewModel.get();

            if (model != null) {
                final Application application = model.getApplication();

                if (model.mArtistID == -1) {
                    if (model.mLoadRecent) {
                        // load recent albums
                        return MusicLibraryHelper.getRecentAlbums(application);
                    } else {
                        // load all albums
                        return MusicLibraryHelper.getAllAlbums(application);
                    }
                } else {
                    // load all albums from the given artist

                    // Read order preference
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(application);
                    String orderKey = sharedPref.getString(application.getString(R.string.pref_album_sort_order_key), application.getString(R.string.pref_artist_albums_sort_default));

                    return MusicLibraryHelper.getAllAlbumsForArtist(model.mArtistID, orderKey, application);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<AlbumModel> result) {
            final AlbumViewModel model = mViewModel.get();

            if (model != null) {
                model.setData(result);
            }
        }
    }

    public static class AlbumViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        private final long mArtistID;

        private final boolean mLoadRecent;

        private AlbumViewModelFactory(final Application application, final long artistID, final boolean loadRecent) {
            mApplication = application;
            mArtistID = artistID;
            mLoadRecent = loadRecent;
        }

        public AlbumViewModelFactory(final Application application, final boolean loadRecent) {
            this(application, -1, loadRecent);
        }

        public AlbumViewModelFactory(final Application application, final long artistID) {
            this(application, artistID, false);
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AlbumViewModel(mApplication, mArtistID, mLoadRecent);
        }
    }
}
