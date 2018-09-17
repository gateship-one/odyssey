/*
 * Copyright (C) 2018 Team Gateship-One
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

package org.gateshipone.odyssey.loaders;

import java.util.List;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.models.ArtistModel;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.loader.content.AsyncTaskLoader;

public class ArtistLoader extends AsyncTaskLoader<List<ArtistModel>> {

    public ArtistLoader(final Context context) {
        super(context);
    }

    /**
     * Load all artists from the mediastore.
     */
    @Override
    public List<ArtistModel> loadInBackground() {
        final Context context = getContext();

        SharedPreferences sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        boolean showAlbumArtistsOnly = sharedPref.getBoolean(context.getString(R.string.pref_album_artists_only_key), context.getResources().getBoolean(R.bool.pref_album_artists_only_default));

        return MusicLibraryHelper.getAllArtists(showAlbumArtistsOnly, context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
