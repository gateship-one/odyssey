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

package org.gateshipone.odyssey.adapter;

import androidx.recyclerview.widget.RecyclerView;

import org.gateshipone.odyssey.models.GenericModel;

import java.util.ArrayList;
import java.util.List;

public abstract class GenericRecyclerViewAdapter<T extends GenericModel, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements ScrollSpeedAdapter {

    /**
     * Variable to store the current scroll speed. Used for image view optimizations
     */
    int mScrollSpeed;

    /**
     * Determines how the new time value affects the average (0.0(new value has no effect) - 1.0(average is only the new value, no smoothing)
     */
    private static final float mSmoothingFactor = 0.3f;

    /**
     * Smoothed average(exponential smoothing) value
     */
    private long mAvgImageTime;

    /**
     * Abstract list with model data used for this adapter.
     */
    private final List<T> mModelData;

    GenericRecyclerViewAdapter() {
        mModelData = new ArrayList<>();
        mScrollSpeed = 0;
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the List or Griditems. Clears old model data.
     *
     * @param data Actual model data
     */
    public void swapModel(final List<T> data) {
        if (data == null) {
            mModelData.clear();
            notifyDataSetChanged();
        } else {
            mModelData.clear();
            mModelData.addAll(data);
            notifyDataSetChanged();
        }
    }

    public T getItem(int position) {
        return mModelData.get(position);
    }

    @Override
    public int getItemCount() {
        return mModelData.size();
    }

    @Override
    abstract public long getItemId(int position);

    abstract public void setItemSize(int size);

    /**
     * Sets the scrollspeed in items per second.
     *
     * @param speed Items per seconds as Integer.
     */
    @Override
    public void setScrollSpeed(int speed) {
        mScrollSpeed = speed;
    }

    /**
     * Returns the smoothed average loading time of images.
     * This value is used by the scrollspeed listener to determine if
     * the scrolling is slow enough to render images (artist, album images)
     *
     * @return Average time to load an image in ms
     */
    @Override
    public long getAverageImageLoadTime() {
        return mAvgImageTime == 0 ? 1 : mAvgImageTime;
    }

    /**
     * This method adds new loading times to the smoothed average.
     * Should only be called from the async cover loader.
     *
     * @param time Time in ms to load a image
     */
    @Override
    public void addImageLoadTime(long time) {
        // Implement exponential smoothing here
        if (mAvgImageTime == 0) {
            mAvgImageTime = time;
        } else {
            mAvgImageTime = (long) (((1 - mSmoothingFactor) * mAvgImageTime) + (mSmoothingFactor * time));
        }
    }
}