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

package org.gateshipone.odyssey.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.GenericViewAdapter;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.utils.AsyncLoader;

public class GridViewItem extends RelativeLayout {
    private static final String TAG = GridViewItem.class.getSimpleName();
    private final AsyncLoader.CoverViewHolder mHolder;

    private AsyncLoader mLoaderTask;

    private final ImageView mImageView;
    private final TextView mTitleView;
    private final ViewSwitcher mSwitcher;

    private boolean mCoverDone = false;

    /**
     * Constructor that already sets the values for each view.
     */
    public GridViewItem(Context context, String title, ViewGroup.LayoutParams layoutParams, GenericViewAdapter adapter) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gridview_item, this, true);

        setLayoutParams(layoutParams);

        mImageView = (ImageView) findViewById(R.id.grid_item_cover_image);
        mTitleView = (TextView) findViewById(R.id.grid_item_title);
        mSwitcher = (ViewSwitcher) findViewById(R.id.grid_item_view_switcher);

        mHolder = new AsyncLoader.CoverViewHolder();
        mHolder.gridItem = this;
        mHolder.mAdapter = adapter;

        setLayoutParams(layoutParams);

        mCoverDone = false;
        mSwitcher.setOutAnimation(null);
        mSwitcher.setInAnimation(null);
        mImageView.setImageDrawable(null);
        mSwitcher.setDisplayedChild(0);
        mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));

        mTitleView.setText(title);
    }

    /**
     * Sets the title for the GridItem
     */
    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    /**
     * Starts the image retrieval task
     */
    public void startCoverImageTask() {
        if (mLoaderTask == null && mHolder.modelItem != null && !mCoverDone) {
            mLoaderTask = new AsyncLoader(getContext());
            mLoaderTask.execute(mHolder);
        }
    }


    public void prepareArtworkFetching(ArtworkManager artworkManager, GenericModel modelItem) {
        if (!modelItem.equals(mHolder.modelItem) || !mCoverDone) {
            setImage(null);
        }
        mHolder.artworkManager = artworkManager;
        mHolder.modelItem = modelItem;

    }

    /**
     * If this GridItem gets detached from the parent it makes no sense to let
     * the task for image retrieval running. (non-Javadoc)
     *
     * @see android.view.View#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mLoaderTask != null) {
            mLoaderTask.cancel(true);
            mLoaderTask = null;
        }
    }

    public void setImage(Bitmap image) {
        if (null != image) {
            mCoverDone = true;

            mImageView.setImageBitmap(image);
            mSwitcher.setDisplayedChild(1);
        } else {
            // Cancel old task
            if (mLoaderTask != null) {
                mLoaderTask.cancel(true);
            }
            mLoaderTask = null;

            mCoverDone = false;

            mSwitcher.setOutAnimation(null);
            mSwitcher.setInAnimation(null);
            mImageView.setImageDrawable(null);
            mSwitcher.setDisplayedChild(0);
            mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);

        if (mHolder != null) {
            mHolder.imageDimension = new Pair<>(params.width, params.height);
        }
    }
}
