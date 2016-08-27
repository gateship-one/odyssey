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

package org.gateshipone.odyssey.utils;


import android.widget.AbsListView;
import android.widget.GridView;

import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;
import org.gateshipone.odyssey.views.GridViewItem;

/**
 * Listener to control image loading while scrolling
 */
public class ScrollSpeedListener implements AbsListView.OnScrollListener {

    private long mLastTime = 0;
    private int mLastFirstVisibleItem = 0;
    private int mScrollSpeed = 0;

    private final ScrollSpeedAdapter mAdapter;
    private final GridView mRootGrid;

    public ScrollSpeedListener(ScrollSpeedAdapter adapter, GridView rootGrid) {
        super();
        mRootGrid = rootGrid;
        mAdapter = adapter;
    }

    /**
     * Callback method to be invoked while the list view or grid view is being scrolled.
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            // if idle load images for all visible items
            mScrollSpeed = 0;
            mAdapter.setScrollSpeed(0);
            for (int i = 0; i <= mRootGrid.getLastVisiblePosition() - mRootGrid.getFirstVisiblePosition(); i++) {
                GridViewItem gridItem = (GridViewItem) mRootGrid.getChildAt(i);
                gridItem.startCoverImageTask();
            }
        }
    }

    /**
     * Callback method to be invoked when the list or grid has been scrolled.
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem != mLastFirstVisibleItem) {
            // view has changed so check if images should be loaded

            // compute scroll speed since last scroll event
            long currentTime = System.currentTimeMillis();
            if (currentTime == mLastTime) {
                return;
            }
            long timeScrollPerRow = currentTime - mLastTime;
            mScrollSpeed = (int) (1000 / timeScrollPerRow);
            mAdapter.setScrollSpeed(mScrollSpeed);

            mLastFirstVisibleItem = firstVisibleItem;
            mLastTime = currentTime;

            // load images only if scroll speed is low
            if (mScrollSpeed < visibleItemCount) {
                for (int i = 0; i < visibleItemCount; i++) {
                    GridViewItem gridItem = (GridViewItem) mRootGrid.getChildAt(i);
                    gridItem.startCoverImageTask();
                }
            }
        }

    }
}
