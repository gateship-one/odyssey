package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.odyssey.models.TrackModel;
import org.odyssey.views.CurrentPlaylistViewItem;

import java.util.ArrayList;

public class CurrentPlaylistListViewAdapter extends BaseAdapter {

    private Context mContext;

    private ArrayList<TrackModel> mTracks;

    public CurrentPlaylistListViewAdapter(Context context) {
        super();

        mContext = context;

        mTracks = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return mTracks.size();
    }

    @Override
    public Object getItem(int position) {
        return mTracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TrackModel track = mTracks.get(position);

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

        return convertView;
    }
}
