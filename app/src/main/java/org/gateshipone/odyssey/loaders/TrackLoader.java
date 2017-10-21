/*
 * Copyright (C) 2017 Team Gateship-One
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

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;

import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.util.List;

public class TrackLoader extends AsyncTaskLoader<List<TrackModel>> {

    protected final Context mContext;

    /**
     * The album key if tracks of a specific album should be loaded.
     */
    private final String mAlbumKey;

    /**
     * The playlist id if tracks of a specific playlist should be loaded.
     */
    private final long mPlaylistID;

    public TrackLoader(Context context) {
        super(context);
        mContext = context;
        mAlbumKey = "";
        mPlaylistID = -1;
    }

    public TrackLoader(Context context, String albumKey) {
        super(context);
        mContext = context;
        mAlbumKey = albumKey;
        mPlaylistID = -1;
    }

    public TrackLoader(Context context, long playlistID) {
        super(context);
        mContext = context;
        mAlbumKey = "";
        mPlaylistID = playlistID;
    }

    /**
     * Load all tracks from the mediastore or a subset if a filter is set.
     */
    @Override
    public List<TrackModel> loadInBackground() {
        if (mPlaylistID != -1) {
            // load playlist tracks
            return MusicLibraryHelper.getTracksForPlaylist(mPlaylistID, mContext);
        } else {
            if (mAlbumKey.isEmpty()) {
                // load all tracks
                return MusicLibraryHelper.getAllTracks(null, mContext);
            } else {
                // load album tracks
                return MusicLibraryHelper.getTracksForAlbum(mAlbumKey, mContext);
            }
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
