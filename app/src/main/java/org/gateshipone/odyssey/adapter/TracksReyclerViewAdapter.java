/*
 * Copyright (C) 2018 Team Team Gateship-One
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

import android.support.annotation.NonNull;
import android.view.ViewGroup;

import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.viewitems.ListViewItem;
import org.gateshipone.odyssey.viewitems.ListViewItemViewHolder;

import java.util.List;

public class TracksReyclerViewAdapter extends GenericRecyclerViewAdapter<TrackModel, ListViewItemViewHolder> {

    private final boolean mShouldShowDiscNumber;

    private boolean mShowDiscNumber;

    public TracksReyclerViewAdapter(final boolean shouldShowDiscNumber) {
        super();
        mShouldShowDiscNumber = shouldShowDiscNumber;
    }

    @NonNull
    @Override
    public ListViewItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListViewItem view = new ListViewItem(parent.getContext(), false, this);
        return new ListViewItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewItemViewHolder holder, int position) {
        final TrackModel track = getItem(position);

        holder.setAlbumTrack(track, mShowDiscNumber);

        holder.itemView.setLongClickable(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getTrackId();
    }

    @Override
    public void swapModel(List<TrackModel> data) {
        super.swapModel(data);

        // check if list contains multiple discs
        if (mShouldShowDiscNumber && data != null && !data.isEmpty()) {
            mShowDiscNumber = (data.get(0).getTrackNumber() / 1000) != (data.get(data.size() - 1).getTrackNumber() / 1000);
        }
    }
}
