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
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.lang.ref.WeakReference;
import java.util.List;

public class ArtistViewModel extends GenericViewModel<ArtistModel> {

    private ArtistViewModel(@NonNull final Application application) {
        super(application);
    }

    @Override
    void loadData() {
        new ArtistLoaderTask(this).execute();
    }

    private static class ArtistLoaderTask extends AsyncTask<Void, Void, List<ArtistModel>> {

        private final WeakReference<ArtistViewModel> mViewModel;

        ArtistLoaderTask(final ArtistViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        protected List<ArtistModel> doInBackground(Void... voids) {
            final ArtistViewModel model = mViewModel.get();

            if (model != null) {
                final Application application = model.getApplication();

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(application);
                boolean showAlbumArtistsOnly = sharedPref.getBoolean(application.getString(R.string.pref_album_artists_only_key), application.getResources().getBoolean(R.bool.pref_album_artists_only_default));

                return MusicLibraryHelper.getAllArtists(showAlbumArtistsOnly, application);
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<ArtistModel> result) {
            final ArtistViewModel model = mViewModel.get();

            if (model != null) {
                model.setData(result);
            }
        }
    }

    public static class ArtistViewModelFactory implements ViewModelProvider.Factory {

        private final Application mApplication;

        public ArtistViewModelFactory(final Application application) {
            mApplication = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ArtistViewModel(mApplication);
        }
    }
}
