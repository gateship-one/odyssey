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

package org.gateshipone.odyssey.adapter;

import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import org.gateshipone.odyssey.models.GenericModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class GenericViewAdapter<T extends GenericModel> extends BaseAdapter implements SectionIndexer, ScrollSpeedAdapter {
    /**
     * Variables used for sectioning (fast scroll).
     */
    private final ArrayList<String> mSectionList;
    private final ArrayList<Integer> mSectionPositions;
    private final HashMap<Character, Integer> mPositionSectionMap;

    private FilterTask mFilterTask = null;

    /**
     * Abstract list with model data used for this adapter.
     */
    private List<T> mModelData;

    /**
     * Abstract list with filtered model data, this list is null if no filter is applied.
     * <p>
     * If not null this list will be used as the model for the adapter.
     */
    private final List<T> mFilteredModelData;

    /**
     * The currently applied filter string.
     */
    private String mFilter;

    /**
     * Variable to store the current scroll speed. Used for image view optimizations
     */
    protected int mScrollSpeed;

    public GenericViewAdapter() {
        super();

        mSectionList = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        mPositionSectionMap = new HashMap<>();

        mModelData = new ArrayList<>();

        mFilteredModelData = new ArrayList<>();

        mFilter = "";
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
        if (data == null) {
            mModelData.clear();
        } else {
            mModelData = data;
        }
        synchronized (mFilteredModelData) {
            mFilteredModelData.clear();
        }

        createSections();

        notifyDataSetChanged();
    }

    /**
     * Looks up the position(index) of a given section(index)
     *
     * @param sectionIndex Section to get the ListView/GridView position for
     * @return The item position of this section start.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    /**
     * Reverse lookup of a section for a given position
     *
     * @param pos Position to get the section for
     * @return Section (index) for the items position
     */
    @Override
    public int getSectionForPosition(int pos) {

        String sectionTitle;

        synchronized (mFilteredModelData) {
            if (!mFilteredModelData.isEmpty()) {
                sectionTitle = mFilteredModelData.get(pos).getSectionTitle();
            } else {
                sectionTitle = mModelData.get(pos).getSectionTitle();
            }
        }

        char itemSection;
        if (sectionTitle.length() > 0) {
            itemSection = sectionTitle.toUpperCase().charAt(0);
        } else {
            itemSection = ' ';
        }

        if (mPositionSectionMap.containsKey(itemSection)) {
            return mPositionSectionMap.get(itemSection);
        }
        return 0;
    }

    /**
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        return mSectionList.toArray();
    }

    /**
     * @return The length of the model data of this adapter.
     */
    @Override
    public int getCount() {
        synchronized (mFilteredModelData) {
            if (!mFilteredModelData.isEmpty() || !mFilter.isEmpty()) {
                return mFilteredModelData.size();
            } else {
                return mModelData.size();
            }
        }
    }

    /**
     * Simple getter for the model data.
     *
     * @param position Index of the item to get. No check for boundaries here.
     * @return The item at index position.
     */
    @Override
    public Object getItem(int position) {
        synchronized (mFilteredModelData) {
            if (!mFilteredModelData.isEmpty() || !mFilter.isEmpty()) {
                return mFilteredModelData.get(position);
            } else {
                return mModelData.get(position);
            }
        }
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

    /**
     * Sets the scrollspeed of the parent GridView. For smoother scrolling
     *
     * @param speed The scrollspeed of the parent GridView.
     */
    public void setScrollSpeed(int speed) {
        mScrollSpeed = speed;
    }

    /**
     * Return the current applied model data.
     * <p>
     * This will be the filtered data if the list is not null.
     *
     * @return The current model
     */
    public List<T> getModelData() {
        synchronized (mFilteredModelData) {
            if (!mFilteredModelData.isEmpty()) {
                return mFilteredModelData;
            } else {
                return mModelData;
            }
        }
    }

    /**
     * Apply the given string as a filter to the model.
     *
     * @param filter The filter string
     */
    public void applyFilter(String filter) {
        if (!filter.equals(mFilter)) {
            mFilter = filter;
        }

        if (mFilterTask != null) {
            mFilterTask.cancel(true);
        }
        mFilterTask = new FilterTask();
        mFilterTask.execute(filter);
    }

    /**
     * Remove a previous filter.
     * <p>
     * This method will clear the filtered model data.
     */
    public void removeFilter() {
        synchronized (mFilteredModelData) {
            mFilteredModelData.clear();
        }
        mFilter = "";

        if (mFilterTask != null) {
            mFilterTask.cancel(true);
            mFilterTask = null;
        }

        createSections();

        notifyDataSetChanged();
    }

    /**
     * Create the section list for the current model for fast scrolling.
     */
    private void createSections() {
        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();

        if (getCount() > 0) {
            GenericModel currentModel = (GenericModel)getItem(0);

            char lastSection;
            if (currentModel.getSectionTitle().length() > 0) {
                lastSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
            } else {
                lastSection = ' ';
            }

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentModel = (GenericModel)getItem(i);

                char currentSection;
                if (currentModel.getSectionTitle().length() > 0) {
                    currentSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
                } else {
                    currentSection = ' ';
                }

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
    }

    /**
     * Async task to perform the filtering of the current model data with the given filter string.
     * <p>
     * The filter will be applied to the section title of the model data.
     */
    private class FilterTask extends AsyncTask<String, Void, Pair<String, List<T>>> {

        @Override
        protected Pair<String, List<T>> doInBackground(String... params) {
            List<T> result = new ArrayList<>();

            String filter = params[0];

            for (T data : mModelData) {
                // Check if task was cancelled from the outside.
                if (isCancelled()) {
                    result.clear();
                    return new Pair<>(filter, result);
                }
                if (data.getSectionTitle().toLowerCase().contains(filter.toLowerCase())) {
                    result.add(data);
                }
            }

            return new Pair<>(filter, result);
        }

        protected void onPostExecute(Pair<String, List<T>> result) {
            if (!isCancelled() && mFilter.equals(result.first)) {
                mFilteredModelData.clear();

                mFilteredModelData.addAll(result.second);

                createSections();

                notifyDataSetChanged();
                if (mFilterTask == this) {
                    mFilterTask = null;
                }
            }
        }
    }
}
