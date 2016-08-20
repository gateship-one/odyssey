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

package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.models.PlaylistModel;
import org.odyssey.views.PlaylistsListViewItem;

public class SavedPlaylistListViewAdapter extends GenericViewAdapter<PlaylistModel> {

    private final Context mContext;

    public SavedPlaylistListViewAdapter(Context context) {
        super();

        mContext = context;
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     *
     * @param position    The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent      The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        PlaylistModel playlist = getModelData().get(position);

        // title
        String playlistTitle = playlist.getPlaylistName();

        if(convertView != null) {
            PlaylistsListViewItem playlistsListViewItem = (PlaylistsListViewItem) convertView;
            playlistsListViewItem.setTitle(playlistTitle);
        } else {
            convertView = new PlaylistsListViewItem(mContext, playlistTitle);
        }

        return convertView;
    }
}
