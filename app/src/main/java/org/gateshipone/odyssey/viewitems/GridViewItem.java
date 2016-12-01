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
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.GenericSectionAdapter;

public class GridViewItem extends GenericImageViewItem {
    private static final String TAG = GridViewItem.class.getSimpleName();

    private final TextView mTitleView;

    /**
     * Constructor that already sets the values for each view.
     */
    public GridViewItem(final Context context, final String title, final GenericSectionAdapter adapter) {
        super(context, R.layout.gridview_item, R.id.grid_item_cover_image, R.id.grid_item_view_switcher, adapter);

        mTitleView = (TextView) findViewById(R.id.grid_item_title);

        mTitleView.setText(title);
    }

    /**
     * Sets the title for the GridItem
     */
    public void setTitle(final String text) {
        mTitleView.setText(text);
    }
}
