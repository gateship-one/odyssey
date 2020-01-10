/*
 * Copyright (C) 2020 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
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
import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;

public class GridViewItem extends GenericImageViewItem {
    private static final String TAG = GridViewItem.class.getSimpleName();

    private final TextView mTitleView;

    /**
     * Base constructor to create a section-type element.
     *
     * @param context The current context.
     * @param adapter The scroll speed adapter for cover loading.
     */
    public GridViewItem(final Context context, final ScrollSpeedAdapter adapter) {
        super(context, R.layout.gridview_item, R.id.grid_item_cover_image, R.id.grid_item_view_switcher, adapter);

        mTitleView = findViewById(R.id.grid_item_title);
    }

    /**
     * Constructor that already sets the title of the gridview.
     *
     * @param context The current context.
     * @param title The title of the item.
     * @param adapter The scroll speed adapter for cover loading.
     */
    public GridViewItem(final Context context, final String title, final ScrollSpeedAdapter adapter) {
        this(context, adapter);

        mTitleView.setText(title);
    }

    /**
     * Sets the title for the GridItem
     */
    public void setTitle(final String text) {
        mTitleView.setText(text);
    }
}
