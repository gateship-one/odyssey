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

package org.gateshipone.odyssey.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Subclass of the standard recyclerview that adds a convenience method for a item clicklistener
 * and adds the option to handle a proper context menu.
 */
public class OdysseyRecyclerView extends RecyclerView {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private RecyclerViewContextMenuInfo mContextMenuInfo;

    public OdysseyRecyclerView(Context context) {
        super(context);
    }

    public OdysseyRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OdysseyRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getChildLayoutPosition(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = getAdapter().getItemId(longPressPosition);
            mContextMenuInfo = new RecyclerViewContextMenuInfo(longPressPosition, longPressId);
            return super.showContextMenuForChild(originalView);
        }
        return false;
    }

    public void addOnItemClicklistener(final OnItemClickListener onItemClickListener) {
        addOnItemTouchListener(new RecyclerViewOnItemClickListener(getContext(), onItemClickListener));
    }

    public static class RecyclerViewContextMenuInfo implements ContextMenu.ContextMenuInfo {

        final public int position;

        final public long id;

        RecyclerViewContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }
    }

    private static class RecyclerViewOnItemClickListener implements RecyclerView.OnItemTouchListener {

        private final OnItemClickListener mOnItemClickListener;

        private final GestureDetector mGestureDetector;

        RecyclerViewOnItemClickListener(Context context, @NonNull OnItemClickListener onItemClickListener) {
            mOnItemClickListener = onItemClickListener;
            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView view, @NonNull MotionEvent motionEvent) {
            final View childView = view.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
            if (childView != null && mGestureDetector.onTouchEvent(motionEvent)) {
                childView.playSoundEffect(SoundEffectConstants.CLICK);
                mOnItemClickListener.onItemClick(view.getChildAdapterPosition(childView));
                return true;
            }
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}
