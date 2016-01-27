package org.odyssey.views;

import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.odyssey.R;
import org.odyssey.adapter.CurrentPlaylistListViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;

public class CurrentPlaylistView extends LinearLayout implements AdapterView.OnItemClickListener{

    private CurrentPlaylistListViewAdapter mCurrentPlaylistListViewAdapter;

    private ListView mListView;

    private Context mContext;

    private PlaybackServiceConnection mPlaybackServiceConnection;

    private OnArtistSelectedListener mArtistSelectedCallback;

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

    public void registerPBServiceConnection(PlaybackServiceConnection playbackServiceConnection){
        mPlaybackServiceConnection = playbackServiceConnection;

        mCurrentPlaylistListViewAdapter = new CurrentPlaylistListViewAdapter(mContext, mPlaybackServiceConnection);

        mListView.setAdapter(mCurrentPlaylistListViewAdapter);
        try {
            mListView.setSelection(mPlaybackServiceConnection.getPBS().getCurrentIndex());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void playlistChanged(NowPlayingInformation info) {
        mCurrentPlaylistListViewAdapter.updateState(info);
        mListView.setSelection(info.getPlayingIndex());
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

    public void removeTrack(int position) {
        try {
            mPlaybackServiceConnection.getPBS().dequeueTrackIndex(position);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void enqueueTrackAsNext(int position) {
        // save track
        TrackModel track = (TrackModel) mCurrentPlaylistListViewAdapter.getItem(position);

        // remove track from playlist
        removeTrack(position);

        try {
            // enqueue removed track as next
            mPlaybackServiceConnection.getPBS().enqueueTrackAsNext(track);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getAlbumKey(int position) {
        TrackModel clickedTrack = (TrackModel) mCurrentPlaylistListViewAdapter.getItem(position);

        String albumKey = clickedTrack.getTrackAlbumKey();

        return albumKey;
    }

    public String getArtistTitle(int position) {
        TrackModel clickedTrack = (TrackModel) mCurrentPlaylistListViewAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        return artistTitle;
    }
}
