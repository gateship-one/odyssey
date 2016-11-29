/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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
import org.gateshipone.odyssey.models.AlbumModel;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;

public class AlbumLoader extends AsyncTaskLoader<List<AlbumModel>> {

    private final Context mContext;

    /**
     * The artist id if albums of a specific artist should be loaded.
     */
    private final long mArtistID;

    public AlbumLoader(Context context) {
        super(context);
        mContext = context;
        mArtistID = -1;
    }

    public AlbumLoader(Context context, long artist) {
        super(context);
        mContext = context;
        mArtistID = artist;
    }

    /**
     * Load all albums from the mediastore or a subset if a filter is set.
     */
    @Override
    public List<AlbumModel> loadInBackground() {
        if (mArtistID == -1) {
            // load all albums
            return MusicLibraryHelper.getAllAlbums(mContext);
        } else {
            // load all albums from the given artist

            // Read order preference
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String orderKey = sharedPref.getString(mContext.getString(R.string.pref_album_sort_order_key), mContext.getString(R.string.pref_artist_albums_sort_default));

            return MusicLibraryHelper.getAllAlbumsForArtist(mArtistID, orderKey, mContext);
        }
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