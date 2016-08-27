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

package org.gateshipone.odyssey.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;

import org.gateshipone.odyssey.adapter.GenericViewAdapter;
import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;

import java.util.List;

abstract public class OdysseyFragment<T extends GenericModel> extends Fragment implements LoaderManager.LoaderCallbacks<List<T>> {

    /**
     * The reference to the possible refresh layout
     */
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * ServiceConnection object to communicate with the PlaybackService
     */
    protected PlaybackServiceConnection mServiceConnection;

    /**
     * The generic adapter for the view model
     */
    protected GenericViewAdapter<T> mAdapter;

    /**
     * Called when the fragment resumes.
     * <p/>
     * Create the PBS connection, reload the data and start the refresh indicator if a refreshlayout exists.
     */
    @Override
    public void onResume() {
        super.onResume();

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    /**
     * Method to reload the data and start the refresh indicator if a refreshlayout exists.
     */
    public void refreshContent() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    /**
     * Called when the loader finished loading its data.
     * <p/>
     * The refresh indicator will be stopped if a refreshlayout exists.
     *
     * @param loader The used loader itself
     * @param model  Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<T>> loader, List<T> model) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }

        mAdapter.swapModel(model);
    }

    /**
     * If the loader is reset, the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<T>> loader) {
        // Clear the model data of the adapter.
        mAdapter.swapModel(null);
    }

    /**
     * Method to apply a filter to the view model of the fragment.
     */
    public void applyFilter(String filter) {
        mAdapter.applyFilter(filter);
    }

    /**
     * Method to remove a previous set filter.
     */
    public void removeFilter() {
        mAdapter.removeFilter();
    }
}
