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
import android.view.ViewGroup;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.utils.ThemeUtils;

public class CurrentPlaylistItem extends GenericImageViewItem {

    protected final TextView mTitleView;
    protected final TextView mSubtitleView;
    protected final TextView mAdditionalSubtitleView;
    protected final TextView mSectionTitleView;
    protected final ViewGroup mSectionHeaderLayout;

    /**
     * Constructor
     *
     * @param context            The current context.
     * @param title              A string for the title of the item.
     * @param subtitle           A string for the subtitle for the item.
     * @param additionalSubtitle A string for the second subtitle for the item.
     * @param headerTitle        A string for the section title or null if item shouldn't contain the section element
     */
    public CurrentPlaylistItem(final Context context, final String title, final String subtitle, final String additionalSubtitle, final String headerTitle) {
        super(context, R.layout.listview_item_current_playlist, R.id.section_header_image, R.id.section_header_image_switcher, null);

        mTitleView = (TextView) findViewById(R.id.item_title);
        mSubtitleView = (TextView) findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = (TextView) findViewById(R.id.item_additional_subtitle);
        mSectionTitleView = (TextView) findViewById(R.id.section_header_text);
        mSectionHeaderLayout = (ViewGroup) findViewById(R.id.section_header);

        mTitleView.setText(title);
        mSubtitleView.setText(subtitle);
        mAdditionalSubtitleView.setText(additionalSubtitle);

        if (headerTitle != null) {
            mSectionTitleView.setText(headerTitle);
            mSectionHeaderLayout.setVisibility(VISIBLE);
        } else {
            mSectionHeaderLayout.setVisibility(GONE);
        }
    }

    /**
     * Sets the title for the item.
     *
     * @param title The title as a string (i.e. a combination of number and title of a track)
     */
    public void setTitle(final String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the subtitle for the item.
     *
     * @param subtitle The subtitle as a string (i.e. a combination of artist and album name of a track)
     */
    public void setSubtitle(final String subtitle) {
        mSubtitleView.setText(subtitle);
    }

    /**
     * Sets the additional subtitle for the item.
     *
     * @param additionalSubtitle The additional subtitle as a string (i.e. the duration of a track)
     */
    public void setAddtionalSubtitle(final String additionalSubtitle) {
        mAdditionalSubtitleView.setText(additionalSubtitle);
    }

    /**
     * Sets the section title or hides the section view and clears the contained image.
     *
     * @param sectionTitle The title of the section or null if section should be hidden.
     */
    public void setSectionTitle(final String sectionTitle) {
        if (sectionTitle != null) {
            mSectionTitleView.setText(sectionTitle);
            mSectionHeaderLayout.setVisibility(VISIBLE);
        } else {
            mSectionHeaderLayout.setVisibility(GONE);
            setImage(null);
        }
    }

    /**
     * Method that tint the title view according to the state.
     *
     * @param state flag indicates if the representing track is currently marked as played by the playbackservice
     */
    public void setPlaying(final boolean state) {
        if (state) {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
            mTitleView.setTextColor(color);
        } else {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_background_primary);
            mTitleView.setTextColor(color);
        }
    }
}
