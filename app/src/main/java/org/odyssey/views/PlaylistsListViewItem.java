package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odyssey.R;

public class PlaylistsListViewItem extends LinearLayout{

    private TextView mTitleView;

    public PlaylistsListViewItem(Context context, String title) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_playlists, this, true);

        mTitleView = (TextView) findViewById(R.id.item_playlists_title);
        mTitleView.setText(title);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }
}
