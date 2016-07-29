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

package org.odyssey.loaders;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import org.odyssey.models.PlaylistModel;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.List;

public class PlaylistLoader extends AsyncTaskLoader<List<PlaylistModel>> {

    private final Context mContext;

    public PlaylistLoader(Context context) {
        super(context);

        mContext = context;
    }

    /*
     * Creates an list of all albums on this device and caches them inside the
     * memory(non-Javadoc)
     *
     * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
     */
    @Override
    public List<PlaylistModel> loadInBackground() {
        Cursor playlistCursor =  PermissionHelper.query(mContext, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionPlaylists, "", null, MediaStore.Audio.Playlists.NAME);

        ArrayList<PlaylistModel> playlists = new ArrayList<>();

        if (playlistCursor != null) {
            int playlistTitleColumnIndex = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            int playlistIDColumnIndex = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._ID);

            for (int i = 0; i < playlistCursor.getCount(); i++) {
                playlistCursor.moveToPosition(i);

                String playlistTitle = playlistCursor.getString(playlistTitleColumnIndex);
                long playlistID = playlistCursor.getLong(playlistIDColumnIndex);

                PlaylistModel playlist = new PlaylistModel(playlistTitle, playlistID);
                playlists.add(playlist);
            }

            playlistCursor.close();
        }
        return playlists;
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
