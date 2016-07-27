package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.models.TrackModel;
import org.odyssey.utils.FormatHelper;
import org.odyssey.views.TracksListViewItem;

public class TracksListViewAdapter extends GenericViewAdapter<TrackModel> {

    private final Context mContext;

    public TracksListViewAdapter(Context context) {
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

        TrackModel track = mModelData.get(position);

        // title
        String trackTitle = track.getTrackName();

        // additional information (artist + album)
        String trackInformation = track.getTrackArtistName() + " - " + track.getTrackAlbumName();

        // tracknumber
        String trackNumber = FormatHelper.formatTrackNumber(track.getTrackNumber());

        // duration
        String trackDuration = FormatHelper.formatTracktimeFromMS(track.getTrackDuration());

        if (convertView != null) {
            TracksListViewItem tracksListViewItem = (TracksListViewItem) convertView;
            tracksListViewItem.setNumber(trackNumber);
            tracksListViewItem.setTitle(trackTitle);
            tracksListViewItem.setAdditionalInformation(trackInformation);
            tracksListViewItem.setDuration(trackDuration);
        } else {
            convertView = new TracksListViewItem(mContext, trackNumber, trackTitle, trackInformation, trackDuration);
        }

        return convertView;
    }
}
