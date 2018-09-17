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
import org.gateshipone.odyssey.models.AlbumModel;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.loader.content.AsyncTaskLoader;

public class AlbumLoader extends AsyncTaskLoader<List<AlbumModel>> {

    /**
     * The artist id if albums of a specific artist should be loaded.
     */
    private final long mArtistID;

    /**
     * Load only the recent albums.
     */
    private final boolean mLoadRecent;

    private AlbumLoader(final Context context, final long artistId, final boolean loadRecent) {
        super(context);
        mArtistID = artistId;
        mLoadRecent = loadRecent;
    }

    public AlbumLoader(final Context context, final long artistId) {
        this(context, artistId, false);
    }

    public AlbumLoader(final Context context, final boolean loadRecent) {
        this(context, -1, loadRecent);
    }

    /**
     * Load all albums from the mediastore or a subset if a filter is set.
     */
    @Override
    public List<AlbumModel> loadInBackground() {
        final Context context = getContext();

        if (mArtistID == -1) {
            if (mLoadRecent) {
                // load recent albums
                return MusicLibraryHelper.getRecentAlbums(context);
            } else {
                // load all albums
                return MusicLibraryHelper.getAllAlbums(context);
            }
        } else {
            // load all albums from the given artist

            // Read order preference
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String orderKey = sharedPref.getString(context.getString(R.string.pref_album_sort_order_key), context.getString(R.string.pref_artist_albums_sort_default));

            return MusicLibraryHelper.getAllAlbumsForArtist(mArtistID, orderKey, context);
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