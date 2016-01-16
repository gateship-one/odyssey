package org.odyssey.views;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.odyssey.R;
import org.odyssey.adapter.CurrentPlaylistListViewAdapter;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackServiceConnection;

public class CurrentPlaylistView extends LinearLayout implements AdapterView.OnItemClickListener{

    private CurrentPlaylistListViewAdapter mCurrentPlaylistListViewAdapter;

    private ListView mListView;

    private Context mContext;

    private PlaybackServiceConnection mPlaybackServiceConnection;

    public CurrentPlaylistView(Context context) {
        this(context,null);
    }

    public CurrentPlaylistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_current_playlist, this, true);

        // get listview
        mListView = (ListView) this.findViewById(R.id.current_playlist_listview);

        // add progressbar
        mListView.setEmptyView(this.findViewById(R.id.albums_progressbar));
        mListView.setOnItemClickListener(this);

        mContext = context;
    }

    public void registerPBServiceConnection(PlaybackServiceConnection playbackServiceConnection) {
        mPlaybackServiceConnection = playbackServiceConnection;

        mCurrentPlaylistListViewAdapter = new CurrentPlaylistListViewAdapter(mContext, mPlaybackServiceConnection);

        mListView.setAdapter(mCurrentPlaylistListViewAdapter);
    }

    public void playlistChanged(NowPlayingInformation info) {
        mCurrentPlaylistListViewAdapter.updateState(info);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            mPlaybackServiceConnection.getPBS().jumpTo(position);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
