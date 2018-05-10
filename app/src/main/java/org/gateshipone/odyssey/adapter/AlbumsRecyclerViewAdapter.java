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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.AbsListView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.viewitems.GenericViewItemHolder;
import org.gateshipone.odyssey.viewitems.GridViewItem;
import org.gateshipone.odyssey.viewitems.ListViewItem;

public class AlbumsRecyclerViewAdapter extends GenericRecyclerViewAdapter<AlbumModel, GenericViewItemHolder> implements ArtworkManager.onNewAlbumImageListener {

    private final ArtworkManager mArtworkManager;

    private final boolean mHideArtwork;

    private final boolean mUseList;

    private int mItemSize;

    public AlbumsRecyclerViewAdapter(final Context context, final boolean useList) {
        super();

        mArtworkManager = ArtworkManager.getInstance(context.getApplicationContext());

        mUseList = useList;
        if (mUseList) {
            mItemSize = (int) context.getResources().getDimension(R.dimen.material_list_item_height);
        } else {
            mItemSize = (int) context.getResources().getDimension(R.dimen.grid_item_height);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mHideArtwork = sharedPreferences.getBoolean(context.getString(R.string.pref_hide_artwork_key), context.getResources().getBoolean(R.bool.pref_hide_artwork_default));
    }

    @NonNull
    @Override
    public GenericViewItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mUseList) {
            final ListViewItem view = new ListViewItem(parent.getContext(), "", this);
            return new GenericViewItemHolder(view);
        } else {
            final GridViewItem view = new GridViewItem(parent.getContext(), "", this);

            if (!mUseList) {
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mItemSize);
                view.setLayoutParams(layoutParams);
            }

            return new GenericViewItemHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull GenericViewItemHolder holder, int position) {
        final AlbumModel album = getItem(position);

        holder.setTitle(album.getAlbumName());

        if (!mUseList) {
            ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            layoutParams.height = mItemSize;
            holder.itemView.setLayoutParams(layoutParams);
        }

        if (!mHideArtwork) {
            // This will prepare the view for fetching the image from the internet if not already saved in local database.
            holder.prepareArtworkFetching(mArtworkManager, album);

            // Check if the scroll speed currently is already 0, then start the image task right away.
            if (mScrollSpeed == 0) {
                holder.setImageDimensions(mItemSize, mItemSize);
                holder.startCoverImageTask();
            }
        }

        holder.itemView.setLongClickable(true);
    }

    public void setItemSize(int size) {
        mItemSize = size;

        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getAlbumID();
    }

    @Override
    public void newAlbumImage(AlbumModel album) {
        notifyDataSetChanged();
    }
}
