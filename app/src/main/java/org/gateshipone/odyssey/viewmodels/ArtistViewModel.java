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
import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.util.List;

import androidx.annotation.NonNull;

public class ArtistViewModel extends GenericViewModel<ArtistModel> {

    public ArtistViewModel(@NonNull final Application application) {
        super(application);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    void loadData() {
        new AsyncTask<Void, Void, List<ArtistModel>>() {

            @Override
            protected List<ArtistModel> doInBackground(Void... voids) {
                final Application application = getApplication();

                SharedPreferences sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(application);
                boolean showAlbumArtistsOnly = sharedPref.getBoolean(application.getString(R.string.pref_album_artists_only_key), application.getResources().getBoolean(R.bool.pref_album_artists_only_default));

                return MusicLibraryHelper.getAllArtists(showAlbumArtistsOnly, application);
            }

            @Override
            protected void onPostExecute(List<ArtistModel> result) {
                setData(result);
            }
        }.execute();
    }
}
