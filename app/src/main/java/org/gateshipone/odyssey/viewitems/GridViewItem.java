/*
 * Copyright (C) 2023 Team Gateship-One
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
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;

public class GridViewItem extends GenericImageViewItem {
    private static final String TAG = GridViewItem.class.getSimpleName();

    private final TextView mTitleView;

    public static GridViewItem createAlbumItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new GridViewItem(context, adapter);
    }

    public static GridViewItem createArtistItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new GridViewItem(context, adapter);
    }

    /**
     * Constructor of the gridview.
     *
     * @param context The current context.
     * @param adapter The scroll speed adapter for cover loading.
     */
    private GridViewItem(final Context context, final ScrollSpeedAdapter adapter) {
        super(context, R.layout.gridview_item, R.id.grid_item_cover_image, R.id.grid_item_view_switcher, adapter);

        mTitleView = findViewById(R.id.grid_item_title);
    }

    /**
     * Extracts the information from a album model.
     *
     * @param album The current album model.
     */
    public void setAlbum(final AlbumModel album) {
        mTitleView.setText(album.getAlbumName());
    }

    /**
     * Extracts the information from a artist model.
     *
     * @param artist The current artist model.
     */
    public void setArtist(final ArtistModel artist) {
        mTitleView.setText(artist.getArtistName());
    }
}
