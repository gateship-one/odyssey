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
import android.os.AsyncTask;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class PlaylistViewModel extends GenericViewModel<PlaylistModel> {

    /**
     * Flag if a header element should be inserted.
     */
    private final boolean mAddHeader;

    private PlaylistViewModel(@NonNull final Application application, final boolean addHeader) {
        super(application);

        mAddHeader = addHeader;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    void loadData() {
        new AsyncTask<Void, Void, List<PlaylistModel>>() {

            @Override
            protected List<PlaylistModel> doInBackground(Void... voids) {
                final Application application = getApplication();

                List<PlaylistModel> playlists = new ArrayList<>();

                if (mAddHeader) {
                    // add a dummy playlist for the choose playlist dialog
                    // this playlist represents the action to create a new playlist in the dialog
                    playlists.add(new PlaylistModel(application.getString(R.string.create_new_playlist), -1));
                }

                playlists.addAll(MusicLibraryHelper.getAllPlaylists(application));

                return playlists;
            }

            @Override
            protected void onPostExecute(List<PlaylistModel> result) {
                setData(result);
            }
        }.execute();
    }

    public static class PlaylistViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        private final boolean mAddHeader;

        public PlaylistViewModelFactory(final Application application, final boolean addHeader) {
            mApplication = application;
            mAddHeader = addHeader;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new PlaylistViewModel(mApplication, mAddHeader);
        }
    }
}
