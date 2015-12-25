package org.odyssey.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.odyssey.R;
import org.odyssey.adapter.AllTracksListViewAdapter;
import org.odyssey.adapter.CurrentPlaylistListViewAdapter;

public class CurrentPlaylistView extends LinearLayout {

    private CurrentPlaylistListViewAdapter mCurrentPlaylistListViewAdapter;

    public CurrentPlaylistView(Context context) {
        this(context,null);
    }

    public CurrentPlaylistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_current_playlist, this, true);

        // get listview
        ListView listView = (ListView) this.findViewById(R.id.current_playlist_listview);

        // add progressbar
        listView.setEmptyView(this.findViewById(R.id.albums_progressbar));

        mCurrentPlaylistListViewAdapter = new CurrentPlaylistListViewAdapter(context);

        listView.setAdapter(mCurrentPlaylistListViewAdapter);
    }
}
