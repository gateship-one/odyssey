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

import java.util.ArrayList;
import java.util.List;

import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.utils.PermissionHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;

public class AlbumLoader extends AsyncTaskLoader<List<AlbumModel>> {

    private final Context mContext;

    /**
     * The artist id if albums of a specific artist should be loaded.
     */
    private final long mArtistID;

    /**
     * Filter string to define the sort order of the albums.
     */
    private final String mOrderBy;

    public AlbumLoader(Context context) {
        super(context);
        mContext = context;
        mArtistID = -1;

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String orderPref = sharedPref.getString("pref_album_sort_order","name");

        switch (orderPref) {
            case "name":
                mOrderBy = MediaStore.Audio.Albums.ALBUM;
                break;
            case "year":
                mOrderBy = MediaStore.Audio.Albums.FIRST_YEAR;
                break;
            default:
                mOrderBy = MediaStore.Audio.Albums.ALBUM;
        }
    }

    public AlbumLoader(Context context, long artist) {
        super(context);
        mContext = context;
        mArtistID = artist;

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String orderPref = sharedPref.getString("pref_album_sort_order","name");

        switch (orderPref) {
            case "name":
                mOrderBy = MediaStore.Audio.Albums.ALBUM;
                break;
            case "year":
                mOrderBy = MediaStore.Audio.Albums.FIRST_YEAR;
                break;
            default:
                mOrderBy = MediaStore.Audio.Albums.ALBUM;
        }
    }

    /**
     * Load all albums from the mediastore or a subset if a filter is set.
     */
    @Override
    public List<AlbumModel> loadInBackground() {
        // Create cursor for content retrieval
        Cursor albumCursor;
        if (mArtistID == -1) {
            // load all albums
            albumCursor = PermissionHelper.query(mContext, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");
        } else {
            // load all albums from the given artist
            albumCursor = PermissionHelper.query(mContext, MediaStore.Audio.Artists.Albums.getContentUri("external", mArtistID), MusicLibraryHelper.projectionAlbums, "", null, mOrderBy + " COLLATE NOCASE");
        }
        ArrayList<AlbumModel> albums = new ArrayList<>();

        if(albumCursor != null) {
            int albumKeyColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
            int albumTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
            int imagePathColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
            int artistTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
            int albumIDColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID);

            for (int i = 0; i < albumCursor.getCount(); i++) {
                albumCursor.moveToPosition(i);
                String albumKey = albumCursor.getString(albumKeyColumnIndex);
                String albumTitle = albumCursor.getString(albumTitleColumnIndex);
                String imagePath = albumCursor.getString(imagePathColumnIndex);
                String artistTitle = albumCursor.getString(artistTitleColumnIndex);
                long albumID = albumCursor.getLong(albumIDColumnIndex);
                AlbumModel album = new AlbumModel(albumTitle, imagePath, artistTitle, albumKey, albumID);
                albums.add(album);

            }
            albumCursor.close();
        }
        return albums;
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