/*
 * Copyright (C) 2017 Team Gateship-One
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

package org.gateshipone.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.viewitems.ListViewItem;

import java.util.List;

public class TracksAdapter extends GenericSectionAdapter<TrackModel> {

    private final Context mContext;

    private boolean mShowDiscNumber;

    private final boolean mShouldShowDiscNumber;

    public TracksAdapter(Context context) {
        this(context, false);
    }

    public TracksAdapter(Context context, boolean shouldShowDiscNumber) {
        super();

        mContext = context;
        mShouldShowDiscNumber = shouldShowDiscNumber;
    }

    @Override
    public void swapModel(List<TrackModel> data) {
        super.swapModel(data);

        // check if list contains multiple discs
        if (mShouldShowDiscNumber && data != null && !data.isEmpty()) {
            mShowDiscNumber = (data.get(0).getTrackNumber() / 1000) != (data.get(data.size() - 1).getTrackNumber() / 1000);
        }
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

        TrackModel track = (TrackModel) getItem(position);

        ListViewItem listViewItem;
        // Check if a view can be recycled
        if (convertView != null) {
            listViewItem = (ListViewItem) convertView;
        } else {
            listViewItem = new ListViewItem(mContext, false, this);
        }

        listViewItem.setAlbumTrack(mContext, track, mShowDiscNumber);

        return listViewItem;
    }
}
