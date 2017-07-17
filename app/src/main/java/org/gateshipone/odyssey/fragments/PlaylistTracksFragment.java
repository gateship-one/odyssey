/*
 * Copyright (C) 2017 Team Gateship-One
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

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.TracksAdapter;
import org.gateshipone.odyssey.loaders.PlaylistTrackLoader;
import org.gateshipone.odyssey.loaders.TrackLoader;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.PermissionHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;

import java.util.List;

public class PlaylistTracksFragment extends OdysseyFragment<TrackModel> implements AdapterView.OnItemClickListener {

    /**
     * Key values for arguments of the fragment
     */
    // FIXME move to separate class to get unified constants?
    public final static String ARG_PLAYLISTTITLE = "playlisttitle";
    public final static String ARG_PLAYLISTID = "playlistid";

    public final static String ARG_PLAYLISTPATH = "playlistpath";

    /**
     * The information of the displayed playlist
     */
    private String mPlaylistTitle = "";
    private long mPlaylistID = -1;

    private String mPlaylistPath;

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_refresh, container, false);

        // get listview
        mListView = (AbsListView) rootView.findViewById(R.id.list_refresh_listview);

        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        mAdapter = new TracksAdapter(getActivity());

        // Disable sections
        mAdapter.enableSections(false);

        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) rootView.findViewById(R.id.empty_view_message)).setText(R.string.empty_tracks_message);

        registerForContextMenu(mListView);

        // activate options menu in toolbar
        setHasOptionsMenu(true);

        Bundle args = getArguments();

        mPlaylistTitle = args.getString(ARG_PLAYLISTTITLE);
        mPlaylistID = args.getLong(ARG_PLAYLISTID);
        mPlaylistPath = args.getString(ARG_PLAYLISTPATH);

        return rootView;
    }

    /**
     * Called when the fragment resumes.
     * Reload the data, setup the toolbar and create the PBS connection.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(mPlaylistTitle, false, false, false);
        }
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<TrackModel>> onCreateLoader(int id, Bundle bundle) {
        if (mPlaylistPath == null) {
            return new TrackLoader(getActivity(), mPlaylistID);
        } else {
            return new PlaylistTrackLoader(getActivity(), mPlaylistPath);
        }
    }

    /**
     * Called when the loader finished loading its data.
     * <p/>
     * The refresh indicator will be stopped if a refreshlayout exists.
     * The FAB will be hidden if the model is empty.
     *
     * @param loader The used loader itself
     * @param model  Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<TrackModel>> loader, List<TrackModel> model) {
        super.onLoadFinished(loader, model);

        if (mToolbarAndFABCallback != null) {
            // set up play button
            if (mAdapter.isEmpty()) {
                mToolbarAndFABCallback.setupFAB(null);
            } else {
                mToolbarAndFABCallback.setupFAB(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPlaylist(0);
                    }
                });
            }
        }
    }

    /**
     * Play the playlist from the current position.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playPlaylist(position);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_playlist_tracks_fragment, menu);

        if(mPlaylistPath != null) {
            // Hide remove track for playlist files as it is unsupported
            menu.findItem(R.id.fragment_playlist_tracks_action_remove).setVisible(false);
        }
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_playlist_tracks_action_enqueue:
                enqueueTrack(info.position);
                return true;
            case R.id.fragment_playlist_tracks_action_enqueueasnext:
                enqueueTrackAsNext(info.position);
                return true;
            case R.id.fragment_playlist_tracks_action_remove:
                removeTrackFromPlaylist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_playlist_tracks_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_add_playlist_tracks).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_playlist_tracks).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_playlist_tracks:
                enqueuePlaylist();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Call the PBS to enqueue the entire playlist.
     */
    private void enqueuePlaylist() {
        try {
            // add the playlist
            if (mPlaylistPath == null) {
                mServiceConnection.getPBS().enqueuePlaylist(mPlaylistID);
            } else {
                mServiceConnection.getPBS().enqueuePlaylistFile(mPlaylistPath);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Call the PBS to play the entire playlist and start with the selected track.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected track in the adapter
     */
    private void playPlaylist(int position) {

        try {
            if (mPlaylistPath == null) {
                mServiceConnection.getPBS().playPlaylist(mPlaylistID, position);
            } else {
                mServiceConnection.getPBS().playPlaylistFile(mPlaylistPath, position);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the selected track.
     *
     * @param position the position of the selected track in the adapter
     */
    private void enqueueTrack(int position) {

        TrackModel track = (TrackModel) mAdapter.getItem(position);

        try {
            mServiceConnection.getPBS().enqueueTrack(track, false);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the selected track as the next track.
     *
     * @param position the position of the selected track in the adapter
     */
    private void enqueueTrackAsNext(int position) {

        TrackModel track = (TrackModel) mAdapter.getItem(position);

        try {
            mServiceConnection.getPBS().enqueueTrack(track, true);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Remove the selected track from the playlist in the mediastore.
     *
     * @param position the position of the selected track in the adapter
     */
    private void removeTrackFromPlaylist(int position) {
        Cursor trackCursor = PermissionHelper.query(getActivity(), MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID), MusicLibraryHelper.projectionPlaylistTracks, "", null, "");

        if (trackCursor != null) {
            if (trackCursor.moveToPosition(position)) {
                String where = MediaStore.Audio.Playlists.Members._ID + "=?";
                String[] whereVal = {trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID))};

                PermissionHelper.delete(getActivity(), MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID), where, whereVal);

                // reload data
                getLoaderManager().restartLoader(0, getArguments(), this);
            }

            trackCursor.close();
        }
    }
}
