package org.odyssey.adapter;

import android.content.Context;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.views.CurrentPlaylistViewItem;

public class CurrentPlaylistListViewAdapter extends BaseAdapter {

    private Context mContext;

    private PlaybackServiceConnection mPlayBackServiceConnection;

    private int mCurrentPlayingIndex = -1;

    private int mPlaylistSize = 0;

    public CurrentPlaylistListViewAdapter(Context context, PlaybackServiceConnection playbackServiceConnection) {
        super();

        mContext = context;

        mPlayBackServiceConnection = playbackServiceConnection;

        try {
            mPlaylistSize = mPlayBackServiceConnection.getPBS().getPlaylistSize();
            mCurrentPlayingIndex = mPlayBackServiceConnection.getPBS().getCurrentIndex();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return mPlaylistSize;
    }

    @Override
    public Object getItem(int position) {
        try {
            if (mPlayBackServiceConnection != null) {
                return mPlayBackServiceConnection.getPBS().getPlaylistSong(position);
            } else {
                return null;
            }
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TrackModel track;
        try {
            if (mPlayBackServiceConnection != null) {
                track = mPlayBackServiceConnection.getPBS().getPlaylistSong(position);
            } else {
                track = new TrackModel();
            }
        } catch (RemoteException e) {
            track = new TrackModel();
        }

        // title
        String trackTitle = track.getTrackName();

        // additional information (artist + album)
        String trackInformation = track.getTrackArtistName() + " - " + track.getTrackAlbumName();

        // tracknumber
        String trackNumber = String.valueOf(track.getTrackNumber());

        if(trackNumber.length() >= 4) {
            trackNumber = trackNumber.substring(2);
        }
        // duration
        String seconds = String.valueOf((track.getTrackDuration() % 60000) / 1000);
        if(seconds.length() == 1) {
            seconds = "0" + seconds;
        }

        String minutes = String.valueOf(track.getTrackDuration() / 60000);

        String trackDuration = minutes + ":" + seconds;

        if(convertView != null) {
            CurrentPlaylistViewItem currentPlaylistViewItem = (CurrentPlaylistViewItem) convertView;
            currentPlaylistViewItem.setNumber(trackNumber);
            currentPlaylistViewItem.setTitle(trackTitle);
            currentPlaylistViewItem.setAdditionalInformation(trackInformation);
            currentPlaylistViewItem.setDuration(trackDuration);
        } else {
            convertView = new CurrentPlaylistViewItem(mContext, trackNumber, trackTitle, trackInformation, trackDuration);
        }

        if(position == mCurrentPlayingIndex) {
            ((CurrentPlaylistViewItem)convertView).setPlaying(true);
        } else {
            ((CurrentPlaylistViewItem)convertView).setPlaying(false);
        }

        return convertView;
    }

    public void updateState(NowPlayingInformation info) {
        mCurrentPlayingIndex = info.getPlayingIndex();
        mPlaylistSize = info.getPlaylistLength();
        notifyDataSetChanged();
    }
}
