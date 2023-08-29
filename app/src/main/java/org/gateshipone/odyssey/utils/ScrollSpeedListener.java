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

package org.gateshipone.odyssey.utils;


import android.widget.AbsListView;
import android.widget.GridView;

import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;
import org.gateshipone.odyssey.viewitems.GenericImageViewItem;

/**
 * Listener to control image loading while scrolling
 */
public class ScrollSpeedListener implements AbsListView.OnScrollListener {

    private long mLastTime = 0;
    private int mLastFirstVisibleItem = 0;

    /**
     * Items per second scrolling over the screen
     */
    private int mScrollSpeed = 0;

    private final ScrollSpeedAdapter mAdapter;

    public ScrollSpeedListener(ScrollSpeedAdapter adapter) {
        super();
        mAdapter = adapter;
    }

    /**
     * Called when a scroll is started/ended and resets the values.
     * If scrolling stops this will start CoverImageTasks
     *
     * @param view        View that has a scrolling state change
     * @param scrollState New scrolling state of the view
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            // if idle load images for all visible items
            mScrollSpeed = 0;
            mAdapter.setScrollSpeed(0);
            for (int i = 0; i <= view.getLastVisiblePosition() - view.getFirstVisiblePosition(); i++) {
                GenericImageViewItem item = (GenericImageViewItem) view.getChildAt(i);
                item.startCoverImageTask();
            }
        }
    }

    /**
     * Called when the associated Listview/GridView is scrolled by the user.
     * This method evaluates if the view is scrolled slow enough to start loading images.
     *
     * @param view             View that is being scrolled.
     * @param firstVisibleItem Index of the first visible item
     * @param visibleItemCount Count of visible items
     * @param totalItemCount   Total item count
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // New row started if this is true.
        if (firstVisibleItem != mLastFirstVisibleItem) {
            long currentTime = System.currentTimeMillis();

            // Calculate the duration of scroll per line
            long timeScrollPerRow = currentTime - mLastTime;

            if (timeScrollPerRow != 0) {
                if (view instanceof GridView) {
                    GridView gw = (GridView) view;
                    mScrollSpeed = (int) (1000 / timeScrollPerRow) * gw.getNumColumns();
                } else {
                    mScrollSpeed = (int) (1000 / timeScrollPerRow);
                }
            } else {
                mScrollSpeed = Integer.MAX_VALUE;
            }

            // Calculate how many items per second of loading images is possible
            int imageLoadingRate = (int) (1000 / mAdapter.getAverageImageLoadTime());

            // Set the scroll speed in the adapter
            mAdapter.setScrollSpeed(mScrollSpeed);

            // Save values for next comparison
            mLastFirstVisibleItem = firstVisibleItem;
            mLastTime = currentTime;
            // Start the grid image loader task only if scroll speed is slow enough:
            // The devices is able to render the images needed for the scroll speed
            if (mScrollSpeed < imageLoadingRate) {
                for (int i = 0; i < visibleItemCount; i++) {
                    GenericImageViewItem item = (GenericImageViewItem) view.getChildAt(i);
                    item.startCoverImageTask();
                }
            }
        }
    }
}
