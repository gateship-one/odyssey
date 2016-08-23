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

public class BookmarksListViewItem extends LinearLayout {

    TextView mTitleView;
    TextView mNumberOfTracksView;
    TextView mDateStringView;

    /**
     * Constructor that only initialize the layout.
     */
    public BookmarksListViewItem(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_bookmarks, this, true);

        mTitleView = (TextView) findViewById(R.id.item_bookmarks_title);
        mNumberOfTracksView = (TextView) findViewById(R.id.item_bookmarks_numberOfTracks);
        mDateStringView = (TextView) findViewById(R.id.item_bookmarks_date);
    }

    /**
     * Constructor that only initialize the layout.
     */
    public BookmarksListViewItem(Context context, String title, String numberOfTracks, String dateString) {
        this(context);

        mTitleView.setText(title);
        mNumberOfTracksView.setText(numberOfTracks);
        mDateStringView.setText(dateString);
    }

    /**
     * Sets the title for the bookmark.
     */
    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the number of tracks in the bookmark.
     */
    public void setNumberOfTracks(String numberOfTracks) {
        mNumberOfTracksView.setText(numberOfTracks);
    }

    /**
     * Sets the date of the bookmark.
     */
    public void setDate(String dateString) {
        mDateStringView.setText(dateString);
    }
}
