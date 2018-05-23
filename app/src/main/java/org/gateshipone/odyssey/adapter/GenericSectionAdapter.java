/*
 * Copyright (C) 2018 Team Gateship-One
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

import android.support.v4.util.Pair;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.utils.FilterTask;
import org.gateshipone.odyssey.utils.SectionCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class GenericSectionAdapter<T extends GenericModel> extends BaseAdapter implements SectionIndexer, ScrollSpeedAdapter {
    private static final String TAG = "GenericSectionAdapter";

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

    private final List<T> mFilteredModelData;

    private String mFilterString;

    private boolean mSectionsEnabled;
    /**
     * Task used to do the filtering of the list asynchronously
     */
    private FilterTask<T> mFilterTask;

    private ReentrantReadWriteLock mLock;

    private final SectionCreator<T> mSectionCreator;

    GenericSectionAdapter() {
        super();

        mLock = new ReentrantReadWriteLock();

        mModelData = new ArrayList<>();

        mFilteredModelData = new ArrayList<>();
        mFilterString = "";

        mSectionsEnabled = true;

        mSectionCreator = provideSectionCreator();

        mScrollSpeed = 0;
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to call.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param data Actual model data
     */
    public void swapModel(List<T> data) {
        mLock.writeLock().lock();
        mFilteredModelData.clear();
        if (data == null) {
            mModelData.clear();
            mLock.writeLock().unlock();
            notifyDataSetChanged();
            return;
        } else {
            mModelData.clear();
            mModelData.addAll(data);
        }
        mLock.writeLock().unlock();

        setScrollSpeed(0);

        if (mFilterString.isEmpty()) {
            // create sectionlist for fastscrolling
            if (mSectionsEnabled) {
                createSections();
            }

            notifyDataSetChanged();
        } else {
            // Refilter the new data
            startFilterTask();
        }
    }

    /**
     * Looks up the position(index) of a given section(index)
     *
     * @param sectionIndex Section to get the ListView/GridView position for
     * @return The item position of this section start.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        if (mSectionsEnabled) {
            return mSectionCreator.getPositionForIndex(sectionIndex);
        } else {
            return 0;
        }
    }

    /**
     * Reverse lookup of a section for a given position
     *
     * @param pos Position to get the section for
     * @return Section (index) for the items position
     */
    @Override
    public int getSectionForPosition(int pos) {
        if (mSectionsEnabled) {
            final T model = getItem(pos);

            return mSectionCreator.getSectionPositionForModel(model);
        }
        return 0;
    }

    /**
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        if (mSectionsEnabled) {
            return mSectionCreator.getSectionList().toArray();
        }
        return null;
    }

    /**
     * @return The length of the modeldata of this adapter.
     */
    @Override
    public int getCount() {
        mLock.readLock().lock();
        int filteredSize = mFilteredModelData.size();
        int normalSize = mModelData.size();
        mLock.readLock().unlock();

        return (filteredSize > 0 || !mFilterString.isEmpty()) ? filteredSize : normalSize;
    }

    /**
     * Simple getter for the model data.
     *
     * @param position Index of the item to get. No check for boundaries here.
     * @return The item at index position.
     */
    @Override
    public T getItem(int position) {
        mLock.readLock().lock();

        int filteredSize = mFilteredModelData.size();

        T obj = filteredSize > 0 ? mFilteredModelData.get(position) : mModelData.get(position);

        mLock.readLock().unlock();

        return obj;
    }

    /**
     * Simple position->id mapping here.
     *
     * @param position Position to get the id from
     * @return The id (position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }


    private void createSections() {
        // Get write lock, to ensure count does not change during execution
        mLock.writeLock().lock();
        mSectionCreator.createSections(mFilteredModelData.size() > 0 ? mFilteredModelData : mModelData);
        mLock.writeLock().unlock();
    }

    public void applyFilter(String filterString) {
        if (!filterString.equals(mFilterString)) {
            mFilterString = filterString;
            startFilterTask();
        }
    }

    private void startFilterTask() {
        if (mFilterTask != null) {
            mFilterTask.cancel(true);
        }
        mFilterTask = provideFilterTask();
        mLock.readLock().lock();
        mFilterTask.execute(mFilterString);
    }

    public void removeFilter() {
        if (!mFilterString.isEmpty()) {
            mLock.writeLock().lock();

            mFilteredModelData.clear();
            mFilterString = "";

            mLock.writeLock().unlock();

            if (mSectionsEnabled) {
                createSections();
            }
            notifyDataSetChanged();
        }
    }

    private void updateAfterFiltering(final Pair<List<T>, String> result) {
        if (result.first != null && mFilterString.equals(result.second)) {
            mLock.readLock().unlock();

            mLock.writeLock().lock();

            mFilteredModelData.clear();
            mFilteredModelData.addAll(result.first);

            mLock.writeLock().unlock();

            setScrollSpeed(0);
            if (mSectionsEnabled) {
                createSections();
            }
            notifyDataSetChanged();
        } else {
            mLock.readLock().unlock();
        }
    }

    private void filteringAborted() {
        mLock.readLock().unlock();
    }

    private FilterTask<T> provideFilterTask() {
        return new FilterTask<>(mModelData,
                provideFilter(),
                this::updateAfterFiltering, this::filteringAborted);
    }

    protected FilterTask.Filter<T> provideFilter() {
        return (elem, filterString) -> elem.getSectionTitle().toLowerCase().contains(filterString.toLowerCase());
    }

    protected SectionCreator<T> provideSectionCreator() {
        return new SectionCreator<>(model -> {
            final String sectionTitle = model.getSectionTitle();

            return sectionTitle.isEmpty() ? ' ' : sectionTitle.toUpperCase().charAt(0);
        });
    }

    /**
     * Allows to enable/disable the support for sections of this adapter.
     * In case of enabling it creates the sections.
     * In case of disabling it will clear the data.
     *
     * @param enabled
     */
    public void enableSections(boolean enabled) {
        mSectionsEnabled = enabled;
        if (mSectionsEnabled) {
            createSections();
        } else {
            mLock.writeLock().lock();
            mSectionCreator.clearSections();
            mLock.writeLock().unlock();
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the scrollspeed in items per second.
     *
     * @param speed Items per seconds as Integer.
     */
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
    public long getAverageImageLoadTime() {
        return mAvgImageTime == 0 ? 1 : mAvgImageTime;
    }

    /**
     * This method adds new loading times to the smoothed average.
     * Should only be called from the async cover loader.
     *
     * @param time Time in ms to load a image
     */
    public void addImageLoadTime(long time) {
        // Implement exponential smoothing here
        if (mAvgImageTime == 0) {
            mAvgImageTime = time;
        } else {
            mAvgImageTime = (long) (((1 - mSmoothingFactor) * mAvgImageTime) + (mSmoothingFactor * time));
        }
    }
}
