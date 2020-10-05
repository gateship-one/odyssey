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

package org.gateshipone.odyssey.adapter;

import android.view.ViewGroup;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewitems.GenericViewItemHolder;
import org.gateshipone.odyssey.viewitems.ListViewItem;

import java.util.List;

import androidx.annotation.NonNull;

public class TracksRecyclerViewAdapter extends GenericRecyclerViewAdapter<TrackModel, GenericViewItemHolder> {

    private final boolean mShouldShowDiscNumber;

    private boolean mShowDiscNumber;

    public TracksRecyclerViewAdapter(final boolean shouldShowDiscNumber) {
        super();
        mShouldShowDiscNumber = shouldShowDiscNumber;
    }

    @NonNull
    @Override
    public GenericViewItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListViewItem view = ListViewItem.createAlbumTrackItem(parent.getContext(), this);

        // set a selectable background manually
        view.setBackgroundResource(ThemeUtils.getThemeResourceId(parent.getContext(), R.attr.selectableItemBackground));
        return new GenericViewItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenericViewItemHolder holder, int position) {
        final TrackModel track = getItem(position);

        holder.setAlbumTrack(track, mShowDiscNumber);

        // We have to set this to make the context menu working with recycler views.
        holder.itemView.setLongClickable(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getTrackId();
    }

    @Override
    public void setItemSize(int size) {
        // method only needed if adapter supports grid view
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
