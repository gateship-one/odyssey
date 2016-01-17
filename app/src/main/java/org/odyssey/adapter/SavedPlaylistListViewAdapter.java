package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.models.PlaylistModel;
import org.odyssey.views.SavedPlaylistsListViewItem;

public class SavedPlaylistListViewAdapter extends GenericViewAdapter<PlaylistModel> {

    private Context mContext;

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
            SavedPlaylistsListViewItem savedPlaylistsListViewItem = (SavedPlaylistsListViewItem) convertView;
            savedPlaylistsListViewItem.setTitle(playlistTitle);
        } else {
            convertView = new SavedPlaylistsListViewItem(mContext, playlistTitle);
        }

        return convertView;
    }
}
