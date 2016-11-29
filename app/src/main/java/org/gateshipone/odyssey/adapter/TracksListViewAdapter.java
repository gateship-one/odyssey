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

package org.gateshipone.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.views.ListViewItem;

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

        TrackModel track = (TrackModel) getItem(position);

        // title (number + name)
        String trackTitle = track.getTrackName();
        String trackNumber = FormatHelper.formatTrackNumber(track.getTrackNumber());
        if (!trackTitle.isEmpty() && !trackNumber.isEmpty()) {
            trackTitle = mContext.getString(R.string.track_title_template, trackNumber, trackTitle);
        } else if (!trackNumber.isEmpty()) {
            trackTitle = trackNumber;
        }

        // subtitle (artist + album)
        String trackSubtitle = track.getTrackAlbumName();
        if (!track.getTrackArtistName().isEmpty() && !trackSubtitle.isEmpty()) {
            trackSubtitle = mContext.getString(R.string.track_title_template, track.getTrackArtistName(), trackSubtitle);
        } else if (!track.getTrackArtistName().isEmpty()) {
            trackSubtitle = track.getTrackArtistName();
        }

        // duration
        String trackDuration = FormatHelper.formatTracktimeFromMS(mContext, track.getTrackDuration());

        if (convertView != null) {
            ListViewItem listViewItem = (ListViewItem) convertView;
            listViewItem.setTitle(trackTitle);
            listViewItem.setSubtitle(trackSubtitle);
            listViewItem.setAddtionalSubtitle(trackDuration);
        } else {
            convertView = new ListViewItem(mContext, trackTitle, trackSubtitle, trackDuration);
        }

        return convertView;
    }
}
