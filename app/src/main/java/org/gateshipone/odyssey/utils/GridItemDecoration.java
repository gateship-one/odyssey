/*
 * Copyright (C) 2019 Team Gateship-One
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

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Helper class to generate spacing between griditems in a recyclerview.
 */
public class GridItemDecoration extends RecyclerView.ItemDecoration {

    /**
     * The offset in pixel for the outer offset of the grid.
     */
    private final int mSpacingOffsetPX;

    /**
     * The offset in pixel for the inner offset of the grid
     */
    private final int mHalfSpacingOffsetPX;

    public GridItemDecoration(final int spacingOffsetPX, final int halfSpacingOffsetPX) {
        mSpacingOffsetPX = spacingOffsetPX;
        mHalfSpacingOffsetPX = halfSpacingOffsetPX;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        final int position = parent.getChildAdapterPosition(view);

        // get the current number of columns for the grid
        final int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();

        final int left = isFirstInRow(position, spanCount) ? 0 : mHalfSpacingOffsetPX;
        final int top = isInFirstRow(position, spanCount) ? 0 : mSpacingOffsetPX;
        final int right = isLastInRow(position, spanCount) ? 0 : mHalfSpacingOffsetPX;

        // the offset for the bottom should always be 0 because the top margin of the next row will apply.
        outRect.set(left, top, right, 0);
    }

    private boolean isInFirstRow(final int position, final int spanCount) {
        return position < spanCount;
    }

    private boolean isFirstInRow(final int position, final int spanCount) {
       return  position % spanCount == 0;
    }

    private boolean isLastInRow(final int position, final int spanCount) {
        return isFirstInRow(position + 1, spanCount);
    }
}
