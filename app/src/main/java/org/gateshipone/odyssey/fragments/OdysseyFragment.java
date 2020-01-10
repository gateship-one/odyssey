/*
 * Copyright (C) 2020 Team Gateship-One
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

package org.gateshipone.odyssey.fragments;

import android.database.DataSetObserver;
import android.view.View;
import android.widget.AbsListView;

import org.gateshipone.odyssey.adapter.GenericSectionAdapter;
import org.gateshipone.odyssey.models.GenericModel;

import java.util.List;

import androidx.annotation.NonNull;

abstract public class OdysseyFragment<T extends GenericModel> extends OdysseyBaseFragment<T> {

    /**
     * The reference to the possible abstract list view
     */
    protected AbsListView mListView;

    /**
     * The reference to the possible empty view which should replace the list view if no data is available
     */
    protected View mEmptyView;

    /**
     * The generic adapter for the view model
     */
    protected GenericSectionAdapter<T> mAdapter;

    /**
     * Observer to be notified if the dataset of the adapter changed.
     */
    private OdysseyDataSetObserver mDataSetObserver;

    /**
     * Called when the fragment resumes.
     * <p/>
     * Create the PBS connection, reload the data and start the refresh indicator if a refreshlayout exists.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (null == mDataSetObserver) {
            mDataSetObserver = new OdysseyDataSetObserver();
        }

        mAdapter.registerDataSetObserver(mDataSetObserver);

        getContent();

        mTrimmingEnabled = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mTrimmingEnabled = true;

        mAdapter.unregisterDataSetObserver(mDataSetObserver);
    }

    @Override
    void swapModel(List<T> model) {
        // Transfer the data to the adapter so that the views can use it
        mAdapter.swapModel(model);
    }

    /**
     * Method to apply a filter to the view model of the fragment.
     */
    public void applyFilter(@NonNull String filter) {
        mAdapter.applyFilter(filter.trim());
    }

    /**
     * Method to remove a previous set filter.
     */
    public void removeFilter() {
        mAdapter.removeFilter();
    }

    /**
     * Method to show or hide the listview according to the state of the adapter.
     */
    private void updateView() {
        if (mListView != null && mEmptyView != null) {
            if (mAdapter.isEmpty()) {
                // show empty message
                mListView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                // show list view
                mListView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }


    /**
     * Private observer class to keep informed if the dataset of the adapter has changed.
     * This will trigger an update of the view.
     */
    private class OdysseyDataSetObserver extends DataSetObserver {

        @Override
        public void onInvalidated() {
            super.onInvalidated();

            updateView();
        }

        @Override
        public void onChanged() {
            super.onChanged();

            updateView();
        }
    }
}
