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

package org.odyssey.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.SavedPlaylistListViewAdapter;
import org.odyssey.listener.OnPlaylistSelectedListener;
import org.odyssey.loaders.PlaylistLoader;
import org.odyssey.models.PlaylistModel;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;

import java.util.List;

public class SavedPlaylistsFragment extends OdysseyFragment implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<List<PlaylistModel>> {

    private OnPlaylistSelectedListener mPlaylistSelectedCallback;

    private SavedPlaylistListViewAdapter mSavedPlaylistListViewAdapter;

    private PlaybackServiceConnection mServiceConnection;

    // Save the last scroll position to resume there
    private int mLastPosition;

    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_linear, container, false);

        // get listview
        mListView = (ListView) rootView.findViewById(R.id.list_linear_listview);

        mSavedPlaylistListViewAdapter = new SavedPlaylistListViewAdapter(getActivity());

        mListView.setAdapter(mSavedPlaylistListViewAdapter);

        mListView.setOnItemClickListener(this);

        registerForContextMenu(mListView);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_saved_playlists), false, true, false);

        activity.setUpPlayButton(null);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPlaylistSelectedCallback = (OnPlaylistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPlaylistSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_saved_playlists), false, true, false);

        activity.setUpPlayButton(null);

        // set up pbs connection
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // Save scroll position
        mLastPosition = position;

        // identify current playlist
        PlaylistModel clickedPlaylist = (PlaylistModel) mSavedPlaylistListViewAdapter.getItem(position);

        String playlistName = clickedPlaylist.getPlaylistName();
        long playlistID = clickedPlaylist.getPlaylistID();

        // open playlistfragment
        mPlaylistSelectedCallback.onPlaylistSelected(playlistName, playlistID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_saved_playlists_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.saved_playlists_context_menu_action_play:
                playPlaylist(info.position);
                return true;
            case R.id.saved_playlists_context_menu_action_delete:
                deletePlaylist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void playPlaylist(int position) {
        // identify current playlist
        PlaylistModel clickedPlaylist = (PlaylistModel) mSavedPlaylistListViewAdapter.getItem(position);

        try {
            // clear the playlist
            mServiceConnection.getPBS().clearPlaylist();

            // add playlist
            mServiceConnection.getPBS().enqueuePlaylist(clickedPlaylist.getPlaylistID());

            // start playback
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void deletePlaylist(int position) {
        // identify current playlist
        PlaylistModel clickedPlaylist = (PlaylistModel) mSavedPlaylistListViewAdapter.getItem(position);

        // delete current playlist
        String where = MediaStore.Audio.Playlists._ID + "=?";
        String[] whereVal = {"" + clickedPlaylist.getPlaylistID()};

        PermissionHelper.delete(getActivity(), MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal);

        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    @Override
    public Loader<List<PlaylistModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new PlaylistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<PlaylistModel>> arg0, List<PlaylistModel> model) {
        mSavedPlaylistListViewAdapter.swapModel(model);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mListView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<PlaylistModel>> arg0) {
        mSavedPlaylistListViewAdapter.swapModel(null);
    }

    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }
}
