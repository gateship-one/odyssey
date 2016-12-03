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
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewitems.ListViewItem;

public class FilesAdapter extends GenericSectionAdapter<FileModel> {

    private final Context mContext;

    public FilesAdapter(Context context) {
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

        FileModel file = (FileModel)getItem(position);

        ListViewItem listViewItem;
        // Check if a view can be recycled
        if (convertView != null) {
            listViewItem = (ListViewItem) convertView;
            listViewItem.setFile(mContext, file);
        } else {
            // Create new view if no reusable is available
            listViewItem = new ListViewItem(mContext, file, this);
        }

        return listViewItem;
    }
}
