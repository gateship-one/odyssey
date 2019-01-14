/*
 * Copyright (C) 2019 Team Gateship-One
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
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.adapter.SavedPlaylistsAdapter;
import org.gateshipone.odyssey.listener.OnPlaylistSelectedListener;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;
import org.gateshipone.odyssey.viewmodels.PlaylistViewModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;

public class SavedPlaylistsFragment extends OdysseyFragment<PlaylistModel> implements AdapterView.OnItemClickListener {

    /**
     * Listener to open a playlist
     */
    private OnPlaylistSelectedListener mPlaylistSelectedCallback;

    /**
     * Save the last scroll position to resume there
     */
    private int mLastPosition = -1;

    public static SavedPlaylistsFragment newInstance() {
        return new SavedPlaylistsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_linear, container, false);

        // get listview
        mListView = rootView.findViewById(R.id.list_linear_listview);

        mAdapter = new SavedPlaylistsAdapter(getActivity());

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) rootView.findViewById(R.id.empty_view_message)).setText(R.string.empty_saved_playlists_message);

        // register for context menu
        registerForContextMenu(mListView);

        // setup observer for the live data
        getViewModel().getData().observe(this, this::onDataReady);

        return rootView;
    }

    @Override
    GenericViewModel<PlaylistModel> getViewModel() {
        return ViewModelProviders.of(this, new PlaylistViewModel.PlaylistViewModelFactory(getActivity().getApplication(), false)).get(PlaylistViewModel.class);
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
            mPlaylistSelectedCallback = (OnPlaylistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPlaylistSelectedListener");
        }
    }

    /**
     * Called when the fragment resumes.
     * Reload the data and create the PBS connection.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_saved_playlists), false, true, false);
            // set up play button
            mToolbarAndFABCallback.setupFAB(null);
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
    protected void onDataReady(List<PlaylistModel> model) {
        super.onDataReady(model);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mListView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    /**
     * Callback when an item in the ListView was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // Save scroll position
        mLastPosition = position;

        // identify current playlist
        PlaylistModel clickedPlaylist = mAdapter.getItem(position);

        String playlistName = clickedPlaylist.getPlaylistName();
        long playlistID = clickedPlaylist.getPlaylistID();

        // open playlistfragment
        mPlaylistSelectedCallback.onPlaylistSelected(playlistName, playlistID);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_saved_playlists_fragment, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.saved_playlists_context_menu_action_play:
                playPlaylist(info.position);
                return true;
            case R.id.saved_playlists_context_menu_action_enqueue:
                enqueuePlaylist(info.position);
                return true;
            case R.id.saved_playlists_context_menu_action_delete:
                deletePlaylist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Call the PBS to enqueue the selected playlist.
     *
     * @param position the position of the selected playlist in the adapter
     */
    private void enqueuePlaylist(int position) {
        // identify current playlist
        PlaylistModel clickedPlaylist = mAdapter.getItem(position);

        try {
            // add playlist
            ((GenericActivity) getActivity()).getPlaybackService().enqueuePlaylist(clickedPlaylist.getPlaylistID());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play the selected playlist.
     *
     * @param position the position of the selected playlist in the adapter
     */
    private void playPlaylist(int position) {
        // identify current playlist
        PlaylistModel clickedPlaylist = mAdapter.getItem(position);

        try {
            // add playlist
            ((GenericActivity) getActivity()).getPlaybackService().playPlaylist(clickedPlaylist.getPlaylistID(), 0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Remove the selected playlist from the mediastore.
     *
     * @param position the position of the selected playlist in the adapter
     */
    private void deletePlaylist(final int position) {
        // identify current playlist
        final PlaylistModel clickedPlaylist = mAdapter.getItem(position);

        // delete current playlist
        final boolean reloadData = MusicLibraryHelper.removePlaylist(clickedPlaylist.getPlaylistID(), getActivity().getApplicationContext());

        if (reloadData) {
            // reload data
            refreshContent();
        }
    }
}
