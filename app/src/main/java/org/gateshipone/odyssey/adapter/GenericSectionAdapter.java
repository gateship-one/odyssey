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

import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.widget.SectionIndexer;

import org.gateshipone.odyssey.models.GenericModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class GenericSectionAdapter<T extends GenericModel> extends ScrollSpeedAdapter implements SectionIndexer {
    private static final String TAG = "GenericSectionAdapter";
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

    protected final List<T> mFilteredModelData;

    private String mFilterString;

    private boolean mSectionsEnabled;
    /**
     * Task used to do the filtering of the list asynchronously
     */
    private FilterTask mFilterTask;

    private ReentrantReadWriteLock mLock;


    public GenericSectionAdapter() {
        super();

        mLock = new ReentrantReadWriteLock();

        mSectionList = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        mPositionSectionMap = new HashMap<>();

        mModelData = new ArrayList<>();

        mFilteredModelData = new ArrayList<>();
        mFilterString = "";

        mSectionsEnabled = true;
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to jll.
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
            if (mFilterTask != null) {
                mFilterTask.cancel(true);
            }
            mFilterTask = new FilterTask();
            mFilterTask.execute(mFilterString);
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
            return mSectionPositions.get(sectionIndex);
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
            String sectionTitle = ((GenericModel) getItem(pos)).getSectionTitle();

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
        return 0;
    }

    /**
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        if (mSectionsEnabled) {
            return mSectionList.toArray();
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
    public Object getItem(int position) {
        mLock.readLock().lock();

        int filteredSize = mFilteredModelData.size();

        Object obj = filteredSize > 0 ? mFilteredModelData.get(position) : mModelData.get(position);

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
        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();

        int count = getCount();
        boolean filtered = mFilteredModelData.size() > 0;

        if (count > 0) {
            GenericModel currentModel = (filtered ? mFilteredModelData.get(0) : mModelData.get(0));

            char lastSection;
            if (currentModel.getSectionTitle().length() > 0) {
                lastSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
            } else {
                lastSection = ' ';
            }

            mSectionList.add(String.valueOf(lastSection));
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < count; i++) {
                currentModel = (filtered ? mFilteredModelData.get(i) : mModelData.get(i));

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
        mLock.writeLock().unlock();
    }

    public void applyFilter(String filterString) {
        if (!filterString.equals(mFilterString)) {
            mFilterString = filterString;
            if (mFilterTask != null) {
                mFilterTask.cancel(true);
            }
            mFilterTask = new FilterTask();
            mFilterTask.execute(filterString);
        }

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

    private class FilterTask extends AsyncTask<String, Object, Pair<List<T>, String>> {

        @Override
        protected Pair<List<T>, String> doInBackground(String... lists) {
            List<T> resultList = new ArrayList<>();

            String filterString = lists[0];
            mLock.readLock().lock();
            for (T elem : mModelData) {
                // Check if task was cancelled from the outside.
                if (isCancelled()) {
                    resultList.clear();
                    mLock.readLock().unlock();
                    return new Pair<>(resultList, filterString);
                }
                if (elem.getSectionTitle().toLowerCase().contains(filterString.toLowerCase())) {
                    resultList.add(elem);
                }
            }
            mLock.readLock().unlock();

            return new Pair<>(resultList, filterString);
        }

        protected void onPostExecute(Pair<List<T>, String> result) {
            if (!isCancelled() && mFilterString.equals(result.second)) {
                mLock.writeLock().lock();

                mFilteredModelData.clear();
                mFilteredModelData.addAll(result.first);

                mLock.writeLock().unlock();

                setScrollSpeed(0);
                if (mSectionsEnabled) {
                    createSections();
                }
                notifyDataSetChanged();
            }
        }

    }

    /**
     * Allows to enable/disable the support for sections of this adapter.
     * In case of enabling it creates the sections.
     * In case of disabling it will clear the data.
     * @param enabled
     */
    public void enableSections(boolean enabled) {
        mSectionsEnabled = enabled;
        if (mSectionsEnabled) {
            createSections();
        } else {
            mLock.writeLock().lock();
            mSectionList.clear();
            mSectionPositions.clear();
            mPositionSectionMap.clear();
            mLock.writeLock().unlock();
        }
        notifyDataSetChanged();
    }
}
