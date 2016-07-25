package org.odyssey.utils;


import android.widget.AbsListView;
import android.widget.GridView;

import org.odyssey.adapter.ScrollSpeedAdapter;
import org.odyssey.views.GridViewItem;

public class ScrollSpeedListener implements AbsListView.OnScrollListener {
    private static String TAG = "ScrollSpeedListener";

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

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            mScrollSpeed = 0;
            mAdapter.setScrollSpeed(0);
            for (int i = 0; i <= mRootGrid.getLastVisiblePosition() - mRootGrid.getFirstVisiblePosition(); i++) {
                GridViewItem gridItem = (GridViewItem) mRootGrid.getChildAt(i);
                gridItem.startCoverImageTask();
            }
        }
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem != mLastFirstVisibleItem) {
            long currentTime = System.currentTimeMillis();
            if (currentTime == mLastTime) {
                return;
            }
            long timeScrollPerRow = currentTime - mLastTime;
            mScrollSpeed = (int) (1000 / timeScrollPerRow);
            mAdapter.setScrollSpeed(mScrollSpeed);

            mLastFirstVisibleItem = firstVisibleItem;
            mLastTime = currentTime;

            if (mScrollSpeed < visibleItemCount) {
                for (int i = 0; i < visibleItemCount; i++) {
                    GridViewItem gridItem = (GridViewItem) mRootGrid.getChildAt(i);
                    gridItem.startCoverImageTask();
                }
            }
        }

    }
}
