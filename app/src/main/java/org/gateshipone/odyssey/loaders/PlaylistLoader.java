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

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.util.ArrayList;
import java.util.List;

public class PlaylistLoader extends AsyncTaskLoader<List<PlaylistModel>> {

    private final Context mContext;

    /**
     * Flag if a header element should be inserted.
     */
    private final boolean mAddHeader;

    public PlaylistLoader(Context context, boolean addHeader) {
        super(context);

        mContext = context;
        mAddHeader = addHeader;
    }

    /**
     * Load all playlists from the mediastore.
     */
    @Override
    public List<PlaylistModel> loadInBackground() {
        List<PlaylistModel> playlists = new ArrayList<>();

        if (mAddHeader) {
            // add a dummy playlist for the choose playlist dialog
            // this playlist represents the action to create a new playlist in the dialog
            playlists.add(new PlaylistModel(mContext.getString(R.string.create_new_playlist), -1));
        }

        playlists.addAll(MusicLibraryHelper.getAllPlaylists(mContext));

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
