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
import android.graphics.Bitmap;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.utils.AsyncLoader;

public abstract class GenericImageViewItem extends RelativeLayout implements CoverLoadable {
    private static final String TAG = GenericImageViewItem.class.getSimpleName();

    private final ImageView mImageView;
    private final ViewSwitcher mSwitcher;

    private AsyncLoader mLoaderTask;
    private boolean mCoverDone = false;

    private final AsyncLoader.CoverViewHolder mHolder;

    /**
     * @param context     The current context.
     * @param layoutID    The id of the layout that should be used.
     * @param imageviewID The id of the image view for the header section.
     * @param switcherID  The id of the view switcher in the header section.
     * @param adapter     The scroll speed adapter or null.
     */
    public GenericImageViewItem(final Context context, @LayoutRes final int layoutID, @IdRes final int imageviewID, @IdRes final int switcherID, final ScrollSpeedAdapter adapter) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layoutID, this, true);

        mImageView = (ImageView) findViewById(imageviewID);
        mSwitcher = (ViewSwitcher) findViewById(switcherID);

        mHolder = new AsyncLoader.CoverViewHolder();
        mHolder.coverLoadable = this;
        mHolder.mAdapter = adapter;

        mCoverDone = false;
        if (mImageView != null && mSwitcher != null) {
            mSwitcher.setOutAnimation(null);
            mSwitcher.setInAnimation(null);
            mImageView.setImageDrawable(null);
            mSwitcher.setDisplayedChild(0);
            mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }
    }

    /**
     * Starts the image retrieval task
     */
    public void startCoverImageTask() {
        if (mLoaderTask == null && mHolder.artworkManager != null && mHolder.modelItem != null && !mCoverDone) {
            if (mImageView != null) {
                mHolder.imageDimension = new Pair<>(mImageView.getWidth(), mImageView.getHeight());
            }
            mLoaderTask = new AsyncLoader(getContext());
            mLoaderTask.execute(mHolder);
        }
    }

    /**
     * Prepares the view to load an image when the scrolling view deems it is ready (scrollspeed slow enough).
     *
     * @param artworkManager ArtworkManager instance used to get the image.
     * @param modelItem      ModelItem to get the image for (Album/Artist)
     */
    public void prepareArtworkFetching(final ArtworkManager artworkManager, final GenericModel modelItem) {
        if (!modelItem.equals(mHolder.modelItem) || !mCoverDone) {
            setImage(null);
        }
        mHolder.artworkManager = artworkManager;
        mHolder.modelItem = modelItem;
    }

    /**
     * If this item gets detached from the parent it makes no sense to let
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

    /**
     * Sets the image of this view with a smooth fading animation.
     * If null is supplied it will reset the cover placeholder image.
     *
     * @param image Image to show inside the view. null will result in the placeholder being shown.
     */
    @Override
    public void setImage(final Bitmap image) {
        if (image != null) {
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
            mHolder.modelItem = null;

            mSwitcher.setOutAnimation(null);
            mSwitcher.setInAnimation(null);
            mImageView.setImageDrawable(null);
            mSwitcher.setDisplayedChild(0);
            mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }
    }
}
