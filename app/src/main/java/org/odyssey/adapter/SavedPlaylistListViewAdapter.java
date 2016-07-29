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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        PlaylistModel playlist = mModelData.get(position);

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
