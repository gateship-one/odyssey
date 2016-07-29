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

package org.odyssey.adapter;

import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import org.odyssey.models.GenericModel;

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

    /**
     * Abstract list with model data used for this adapter.
     */
    protected List<T> mModelData;

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
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to call.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param data
     *            Actual model data
     */
    public void swapModel(List<T> data) {
        if (data == null) {
            mModelData.clear();
        } else {
            mModelData = data;
        }
        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mModelData.size() > 0) {
            GenericModel currentModel = mModelData.get(0);

            char lastSection;
            if ( currentModel.getSectionTitle().length() > 0 ) {
                lastSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
            } else {
                lastSection = ' ';
            }

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentModel = mModelData.get(i);

                char currentSection;
                if ( currentModel.getSectionTitle().length() > 0 ) {
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
        notifyDataSetChanged();
    }

    /**
     * Looks up the position(index) of a given section(index)
     * @param sectionIndex Section to get the ListView/GridView position for
     * @return The item position of this section start.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    /**
     * Reverse lookup of a section for a given position
     * @param pos Position to get the section for
     * @return Section (index) for the items position
     */
    @Override
    public int getSectionForPosition(int pos) {

        String sectionTitle = mModelData.get(pos).getSectionTitle();

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
     *
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        return mSectionList.toArray();
    }

    /**
     *
     * @return The length of the model data of this adapter.
     */
    @Override
    public int getCount() {
        return mModelData.size();
    }

    /**
     * Simple getter for the model data.
     * @param position Index of the item to get. No check for boundaries here.
     * @return The item at index position.
     */
    @Override
    public Object getItem(int position) {
        return mModelData.get(position);
    }

    /**
     * Simple position->id mapping here.
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
}
