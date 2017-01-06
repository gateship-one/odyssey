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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.TracksAdapter;
import org.gateshipone.odyssey.listener.OnArtistSelectedListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.loaders.TrackLoader;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;

import java.util.List;

public class AlbumTracksFragment extends OdysseyFragment<TrackModel> implements AdapterView.OnItemClickListener, CoverBitmapLoader.CoverBitmapListener {

    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    /**
     * Key values for arguments of the fragment
     */
    // FIXME move to separate class to get unified constants?
    public final static String ARG_ALBUMKEY = "albumkey";
    public final static String ARG_ALBUMTITLE = "albumtitle";
    public final static String ARG_ALBUMART = "albumart";
    public final static String ARG_ALBUMARTIST = "albumartist";

    /**
     * The information of the displayed album
     */
    private String mAlbumTitle = "";
    private String mAlbumArtURL = "";
    private String mArtistName = "";
    private String mAlbumKey = "";

    private CoverBitmapLoader mBitmapLoader;

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_linear, container, false);

        // get listview
        mListView = (AbsListView) rootView.findViewById(R.id.list_linear_listview);

        mAdapter = new TracksAdapter(getActivity());

        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) rootView.findViewById(R.id.empty_view_message)).setText(R.string.empty_tracks_message);

        registerForContextMenu(mListView);

        // set up toolbar
        Bundle args = getArguments();

        mAlbumTitle = args.getString(ARG_ALBUMTITLE);
        mArtistName = args.getString(ARG_ALBUMARTIST);
        mAlbumArtURL = args.getString(ARG_ALBUMART);
        mAlbumKey = args.getString(ARG_ALBUMKEY);

        setHasOptionsMenu(true);

        mBitmapLoader = new CoverBitmapLoader(getContext(), this);

        return rootView;
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mArtistSelectedCallback = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }

        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }
    }

    /**
     * Called when the fragment resumes.
     * <p/>
     * Set up toolbar and play button.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(mAlbumTitle, false, false, false);
            // set up play button
            mToolbarAndFABCallback.setupFAB(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playAlbum(0);
                }
            });
        }

        AlbumModel album = new AlbumModel(mAlbumTitle, mAlbumArtURL, mArtistName, mAlbumKey, -1);
        mBitmapLoader.getAlbumImage(album);
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
        return new TrackLoader(getActivity(), mAlbumKey);
    }

    /**
     * Play the album from the current position.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playAlbum(position);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_album_tracks_fragment, menu);
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
            case R.id.fragment_album_tracks_action_enqueue:
                enqueueTrack(info.position, false);
                return true;
            case R.id.fragment_album_tracks_action_enqueueasnext:
                enqueueTrack(info.position, true);
                return true;
            case R.id.fragment_album_tracks_action_showartist:
                showArtist(info.position);
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
        menuInflater.inflate(R.menu.options_menu_album_tracks_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_add_album).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_album).setIcon(drawable);

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
            case R.id.action_add_album:
                enqueueAlbum();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Open a fragment for the artist of the selected album.
     *
     * @param position the position of the selected album in the adapter
     */
    private void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = (TrackModel) mAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, getActivity());

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artistTitle, artistID);
    }

    /**
     * Call the PBS to enqueue the selected track.
     *
     * @param position the position of the selected track in the adapter
     * @param asNext   flag if the track should be enqueued as next
     */
    private void enqueueTrack(int position, boolean asNext) {

        TrackModel track = (TrackModel) mAdapter.getItem(position);

        try {
            mServiceConnection.getPBS().enqueueTrack(track, asNext);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the complete album.
     */
    private void enqueueAlbum() {
        // Enqueue complete album

        try {
            mServiceConnection.getPBS().enqueueAlbum(mAlbumKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play the complete album and starts with the selected track.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected track in the adapter
     */
    private void playAlbum(int position) {
        // clear playlist and play current album

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueAlbum();
            mServiceConnection.getPBS().jumpTo(position);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public void receiveBitmap(final Bitmap bm) {
        if (bm != null && mToolbarAndFABCallback != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // set toolbar behaviour and title
                    mToolbarAndFABCallback.setupToolbar(mAlbumTitle, false, false, true);
                    // set toolbar image
                    mToolbarAndFABCallback.setupToolbarImage(bm);
                }
            });
        }
    }
}
