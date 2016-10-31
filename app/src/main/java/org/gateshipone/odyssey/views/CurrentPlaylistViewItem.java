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
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.utils.ThemeUtils;

public class CurrentPlaylistViewItem extends TracksListViewItem {

    private final TextView mSeparatorView;

    /**
     * Constructor that only initialize the layout.
     */
    public CurrentPlaylistViewItem(Context context) {
        super(context);

        mSeparatorView = (TextView) findViewById(R.id.item_tracks_separator);
    }

    /**
     * Constructor that already sets the values for each view.
     */
    public CurrentPlaylistViewItem(Context context, String number, String title, String information, String duration) {
        this(context);

        mTitleView.setText(title);
        mNumberView.setText(number);
        mInformationView.setText(information);
        mDurationView.setText(duration);
    }

    /**
     * Method that tint the title, number and separator view according to the state.
     * @param state flag indicates if the representing track is currently marked as played by the playbackservice
     */
    public void setPlaying(boolean state) {
        if(state) {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
            mTitleView.setTextColor(color);
            mNumberView.setTextColor(color);
            mSeparatorView.setTextColor(color);
        } else {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_primary);
            mTitleView.setTextColor(color);
            mNumberView.setTextColor(color);
            mSeparatorView.setTextColor(color);
        }

    }
}
