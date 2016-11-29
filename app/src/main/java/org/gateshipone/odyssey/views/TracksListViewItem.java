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

package org.gateshipone.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.utils.ThemeUtils;

public class TracksListViewItem extends LinearLayout {

    protected final TextView mTitleView;
    protected final TextView mSubtitleView;
    protected final TextView mTrackDurationView;

    /**
     * Constructor that only initialize the layout.
     *
     * @param context The current android context.
     */
    public TracksListViewItem(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_tracks, this, true);

        mTitleView = (TextView) findViewById(R.id.item_tracks_title);
        mSubtitleView = (TextView) findViewById(R.id.item_tracks_subtitle);
        mTrackDurationView = (TextView) findViewById(R.id.item_tracks_duration);
    }

    /**
     * Constructor that already sets the values for each view.
     *
     * @param context  The current android context.
     * @param title    The title as a string (i.e. a combination of track number and title)
     * @param subtitle The subtitle as a string (i.e. a combination of artist and album name)
     * @param duration The duration of the track as a string
     */
    public TracksListViewItem(Context context, String title, String subtitle, String duration) {
        this(context);

        mTitleView.setText(title);
        mSubtitleView.setText(subtitle);
        mTrackDurationView.setText(duration);
    }

    /**
     * Sets the title for the track.
     *
     * @param title The title as a string (i.e. a combination of track number and title)
     */
    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the subtitle for the track.
     *
     * @param subtitle The subtitle as a string (i.e. a combination of artist and album name)
     */
    public void setSubtitle(String subtitle) {
        mSubtitleView.setText(subtitle);
    }

    /**
     * Sets the duration text for the track.
     *
     * @param duration The duration of the track as a string
     */
    public void setDuration(String duration) {
        mTrackDurationView.setText(duration);
    }

    /**
     * Method that tint the title, number and separator view according to the state.
     *
     * @param state flag indicates if the representing track is currently marked as played by the playbackservice
     */
    public void setPlaying(boolean state) {
        if (state) {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
            mTitleView.setTextColor(color);
        } else {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_background_primary);
            mTitleView.setTextColor(color);
        }
    }
}
