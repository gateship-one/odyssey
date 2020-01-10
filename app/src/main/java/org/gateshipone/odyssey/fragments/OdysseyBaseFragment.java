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

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;

import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

abstract public class OdysseyBaseFragment<T extends GenericModel> extends Fragment {

    /**
     * Callback to setup toolbar and fab
     */
    protected ToolbarAndFABCallback mToolbarAndFABCallback;

    /**
     * The reference to the possible refresh layout
     */
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * Holds if trimming for this Fragment is currently allowed or not.
     */
    protected boolean mTrimmingEnabled;

    /**
     * Callback to check the current memory state
     */
    private OdysseyComponentCallback mComponentCallback;

    /**
     * Holds if data is ready of has to be refetched (e.g. after memory trimming)
     */
    private boolean mDataReady;

    abstract void swapModel(List<T> model);

    abstract GenericViewModel<T> getViewModel();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (null == mComponentCallback) {
            mComponentCallback = new OdysseyComponentCallback();
        }

        // Register the memory trim callback with the system.
        context.registerComponentCallbacks(mComponentCallback);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Unregister the memory trim callback with the system.
        getActivity().getApplicationContext().unregisterComponentCallbacks(mComponentCallback);
    }

    /**
     * Method to reload the data and start the refresh indicator if a refreshlayout exists.
     */
    public void refreshContent() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));
        }

        mDataReady = false;

        getViewModel().reloadData();
    }

    /**
     * Checks if data is available or not. If not it will start getting the data.
     * This method should be called from onResume and if the fragment is part of an view pager,
     * every time the View is activated because the underlying data could be cleaned because
     * of memory pressure.
     */
    public void getContent() {
        // Check if data was fetched already or not (or removed because of trimming)
        if (!mDataReady) {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));
            }

            getViewModel().reloadData();
        }
    }

    /**
     * Called when the observed {@link androidx.lifecycle.LiveData} is changed.
     * <p>
     * This method will update the related adapter and the {@link SwipeRefreshLayout} if present.
     *
     * @param model The data observed by the {@link androidx.lifecycle.LiveData}.
     */
    protected void onDataReady(List<T> model) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(false));
        }

        // Indicate that the data is ready now.
        mDataReady = model != null;

        swapModel(model);
    }

    /**
     * This method can be used to prevent one fragment from triming its necessary data (e.g. active in a pager)
     *
     * @param enabled Enable the memory trimming
     */
    public void enableMemoryTrimming(boolean enabled) {
        mTrimmingEnabled = enabled;
    }

    /**
     * Private callback class used to monitor the memory situation of the system.
     * If memory reaches a certain point, we will relinquish our data.
     */
    private class OdysseyComponentCallback implements ComponentCallbacks2 {

        @Override
        public void onTrimMemory(int level) {
            if (mTrimmingEnabled && level >= TRIM_MEMORY_RUNNING_LOW) {
                getViewModel().clearData();

                mDataReady = false;
            }
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
        }
    }
}
