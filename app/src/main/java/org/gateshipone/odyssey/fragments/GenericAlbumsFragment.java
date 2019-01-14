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

package org.gateshipone.odyssey.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.adapter.AlbumsAdapter;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.listener.OnAlbumSelectedListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.utils.ScrollSpeedListener;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewitems.GenericImageViewItem;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public abstract class GenericAlbumsFragment extends OdysseyFragment<AlbumModel> implements AdapterView.OnItemClickListener {

    /**
     * Listener to open an album
     */
    protected OnAlbumSelectedListener mAlbumSelectedCallback;

    /**
     * Save the last scroll position to resume there
     */
    protected int mLastPosition = -1;

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String viewAppearance = sharedPref.getString(getString(R.string.pref_view_library_key), getString(R.string.pref_library_view_default));

        boolean useList = viewAppearance.equals(getString(R.string.pref_library_view_list_key));

        if (useList) {
            rootView = inflater.inflate(R.layout.list_refresh, container, false);
            // get listview
            mListView = rootView.findViewById(R.id.list_refresh_listview);
        } else {
            rootView = inflater.inflate(R.layout.grid_refresh, container, false);
            // get gridview
            mListView = rootView.findViewById(R.id.grid_refresh_gridview);
        }

        // get swipe layout
        mSwipeRefreshLayout = rootView.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshContent);

        mAdapter = new AlbumsAdapter(getActivity(), mListView, useList);

        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(new ScrollSpeedListener(mAdapter));
        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) rootView.findViewById(R.id.empty_view_message)).setText(R.string.empty_albums_message);

        // register for context menu
        registerForContextMenu(mListView);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewAlbumImageListener((AlbumsAdapter) mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewAlbumImageListener((AlbumsAdapter) mAdapter);
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mAlbumSelectedCallback = (OnAlbumSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnAlbumSelectedListener");
        }

        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }
    }

    /**
     * Called when the observed {@link androidx.lifecycle.LiveData} is changed.
     * <p>
     * This method will update the related adapter and the {@link androidx.swiperefreshlayout.widget.SwipeRefreshLayout} if present.
     *
     * @param model The data observed by the {@link androidx.lifecycle.LiveData}.
     */
    @Override
    protected void onDataReady(List<AlbumModel> model) {
        super.onDataReady(model);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mListView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    /**
     * Callback when an item in the GridView was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // save last scroll position
        mLastPosition = position;

        // identify current album
        AlbumModel currentAlbum = mAdapter.getItem(position);

        Bitmap bitmap = null;

        // Check if correct view type, to be safe
        if (view instanceof GenericImageViewItem) {
            bitmap = ((GenericImageViewItem) view).getBitmap();
        }

        // send the event to the host activity
        mAlbumSelectedCallback.onAlbumSelected(currentAlbum, bitmap);
    }

    /**
     * Call the PBS to enqueue the selected album.
     *
     * @param position the position of the selected album in the adapter
     */
    protected void enqueueAlbum(int position) {
        // identify current album
        AlbumModel clickedAlbum = mAdapter.getItem(position);
        String albumKey = clickedAlbum.getAlbumKey();

        // enqueue album
        try {
            ((GenericActivity) getActivity()).getPlaybackService().enqueueAlbum(albumKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play the selected album.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected album in the adapter
     */
    protected void playAlbum(int position) {
        // identify current album
        AlbumModel clickedAlbum = mAdapter.getItem(position);
        String albumKey = clickedAlbum.getAlbumKey();

        // play album
        try {
            ((GenericActivity) getActivity()).getPlaybackService().playAlbum(albumKey, 0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
