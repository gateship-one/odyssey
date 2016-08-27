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
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.PermissionHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v7.preference.PreferenceManager;

public class ArtistLoader extends AsyncTaskLoader<List<ArtistModel>> {

    private final Context mContext;

    /**
     * Flag if only album artists should be loaded.
     */
    private final boolean mShowAlbumArtistsOnly;

    public ArtistLoader(Context context) {
        super(context);
        this.mContext = context;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        mShowAlbumArtistsOnly = sharedPref.getBoolean("pref_key_album_artists_only", true);
    }

    /**
     * Load all artists from the mediastore.
     */
    @Override
    public List<ArtistModel> loadInBackground() {
        ArrayList<ArtistModel> artists = new ArrayList<>();
        String artist, coverPath;
        if (!mShowAlbumArtistsOnly) {
            // load all artists

            // get all album covers
            Cursor cursorAlbumArt = PermissionHelper.query(mContext, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM}, MediaStore.Audio.Albums.ALBUM_ART + "<>\"\" ) GROUP BY (" + MediaStore.Audio.Albums.ARTIST, null,
                    MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC");

            // get all artists
            Cursor cursorArtists = PermissionHelper.query(mContext, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE ASC");

            if (cursorAlbumArt != null && cursorArtists != null) {
                // join both cursor if match is found
                long artistID;

                int artistTitleColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
                int artistIDColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists._ID);

                int albumArtistTitleColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
                int albumCoverPathColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

                if (cursorAlbumArt.getCount() > 0) {
                    cursorAlbumArt.moveToPosition(0);
                }
                for (int i = 0; i < cursorArtists.getCount(); i++) {
                    cursorArtists.moveToPosition(i);

                    artist = cursorArtists.getString(artistTitleColumnIndex);
                    artistID = cursorArtists.getLong(artistIDColumnIndex);

                    if (cursorAlbumArt.getString(albumArtistTitleColumnIndex).equals(cursorArtists.getString(artistTitleColumnIndex))) {
                        // Found right album art
                        coverPath = cursorAlbumArt.getString(albumCoverPathColumnIndex);
                        if (!cursorAlbumArt.isLast()) {
                            cursorAlbumArt.moveToNext();
                        }
                    } else {
                        coverPath = null;
                    }

                    artists.add(new ArtistModel(artist, coverPath, artistID));
                }

                // return new custom cursor
                cursorAlbumArt.close();
                cursorArtists.close();
            }
        } else {
            // load only artist which has an album entry

            // get all album covers
            Cursor cursorAlbumArt = PermissionHelper.query(mContext, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM}, MediaStore.Audio.Albums.ARTIST + "<>\"\" ) GROUP BY (" + MediaStore.Audio.Albums.ARTIST, null,
                    MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC");

            if (cursorAlbumArt != null) {
                int albumArtistTitleColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
                int albumCoverPathColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

                for (int i = 0; i < cursorAlbumArt.getCount(); i++) {
                    cursorAlbumArt.moveToPosition(i);

                    artist = cursorAlbumArt.getString(albumArtistTitleColumnIndex);
                    coverPath = cursorAlbumArt.getString(albumCoverPathColumnIndex);

                    artists.add(new ArtistModel(artist, coverPath, -1));
                }
                cursorAlbumArt.close();
            }
        }
        return artists;
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
