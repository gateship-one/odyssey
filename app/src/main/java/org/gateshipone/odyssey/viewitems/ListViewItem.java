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

package org.gateshipone.odyssey.viewitems;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.utils.ThemeUtils;

public class ListViewItem extends LinearLayout {

    protected final TextView mTitleView;
    protected final TextView mSubtitleView;
    protected final TextView mAdditionalSubtitleView;

    /**
     * Constructor that only initialize the layout.
     *
     * @param context The current android context.
     */
    public ListViewItem(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item, this, true);

        mTitleView = (TextView) findViewById(R.id.item_title);
        mSubtitleView = (TextView) findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = (TextView) findViewById(R.id.item_additional_subtitle);
    }

    /**
     * Constructor that already sets the values for each view.
     *
     * @param context            The current android context.
     * @param title              The title as a string (i.e. a combination of number and title of a track)
     * @param subtitle           The subtitle as a string (i.e. a combination of artist and album name of a track)
     * @param additionalSubtitle The additional subtitle as a string (i.e. the duration of a track)
     */
    public ListViewItem(Context context, String title, String subtitle, String additionalSubtitle) {
        this(context);

        mTitleView.setText(title);
        mSubtitleView.setText(subtitle);
        mAdditionalSubtitleView.setText(additionalSubtitle);
    }

    /**
     * Sets the title for the item.
     *
     * @param title The title as a string (i.e. a combination of number and title of a track)
     */
    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the subtitle for the item.
     *
     * @param subtitle The subtitle as a string (i.e. a combination of artist and album name of a track)
     */
    public void setSubtitle(String subtitle) {
        mSubtitleView.setText(subtitle);
    }

    /**
     * Sets the additional subtitle for the item.
     *
     * @param additionalSubtitle The additional subtitle as a string (i.e. the duration of a track)
     */
    public void setAddtionalSubtitle(String additionalSubtitle) {
        mAdditionalSubtitleView.setText(additionalSubtitle);
    }

    /**
     * Method that tint the title view according to the state.
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
