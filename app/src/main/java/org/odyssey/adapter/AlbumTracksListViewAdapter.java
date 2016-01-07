package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.models.TrackModel;
import org.odyssey.views.AlbumTracksListViewItem;

public class AlbumTracksListViewAdapter extends GenericViewAdapter<TrackModel> {

    private Context mContext;

    public AlbumTracksListViewAdapter(Context context) {
        super();

        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TrackModel track = mModelData.get(position);

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
            AlbumTracksListViewItem allTracksListViewItem = (AlbumTracksListViewItem) convertView;
            allTracksListViewItem.setNumber(trackNumber);
            allTracksListViewItem.setTitle(trackTitle);
            allTracksListViewItem.setAdditionalInformation(trackInformation);
            allTracksListViewItem.setDuration(trackDuration);
        } else {
            convertView = new AlbumTracksListViewItem(mContext, trackNumber, trackTitle, trackInformation, trackDuration);
        }

        return convertView;
    }
}
