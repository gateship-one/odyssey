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

package org.odyssey.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.R;
import org.odyssey.models.FileModel;
import org.odyssey.utils.FormatHelper;
import org.odyssey.utils.ThemeUtils;
import org.odyssey.views.FilesListViewItem;

public class FilesListViewAdapter extends GenericViewAdapter<FileModel> {

    private final Context mContext;

    public FilesListViewAdapter(Context context) {
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

        FileModel file = mModelData.get(position);

        // title
        String title = file.getName();

        // get icon for filetype
        Drawable icon;
        if (file.isDirectory()) {
            // choose directory icon
            icon = mContext.getDrawable(R.drawable.ic_folder_48dp);
        } else {
            // choose file icon
            icon = mContext.getDrawable(R.drawable.ic_file_48dp);
        }

        if (icon != null) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(mContext, android.R.attr.textColor);
            // tint the icon
            DrawableCompat.setTint(icon, tintColor);
        }

        // last modified
        String lastModifiedDateString = FormatHelper.formatTimeStampToString(file.getLastModified());

        // Check if a view can be recycled
        if (convertView != null) {
            FilesListViewItem filesListViewItem = (FilesListViewItem) convertView;
            filesListViewItem.setTitle(title);
            filesListViewItem.setModifiedDate(lastModifiedDateString);
            filesListViewItem.setIcon(icon);
        } else {
            // Create new view if no reusable is available
            convertView = new FilesListViewItem(mContext, title, lastModifiedDateString, icon);
        }

        return convertView;
    }
}
