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

package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odyssey.R;

public class BookmarksListViewItem extends LinearLayout {

    TextView mTitleView;
    TextView mNumberOfTracksView;
    TextView mDateStringView;

    public BookmarksListViewItem(Context context, String title, String numberOfTracks, String dateString) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_bookmarks, this, true);

        mTitleView = (TextView) findViewById(R.id.item_bookmarks_title);
        mTitleView.setText(title);

        mNumberOfTracksView = (TextView) findViewById(R.id.item_bookmarks_numberOfTracks);
        mNumberOfTracksView.setText(numberOfTracks);

        mDateStringView = (TextView) findViewById(R.id.item_bookmarks_date);
        mDateStringView.setText(dateString);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    public void setNumberOfTracks(String numberOfTracks) {
        mNumberOfTracksView.setText(numberOfTracks);
    }

    public void setDate(String dateString) {
        mDateStringView.setText(dateString);
    }
}
