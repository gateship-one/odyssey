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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import org.gateshipone.odyssey.adapter.ArtistsAdapter;
import org.gateshipone.odyssey.artwork.ArtworkManager;
import org.gateshipone.odyssey.listener.OnArtistSelectedListener;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.ScrollSpeedListener;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewitems.GenericImageViewItem;
import org.gateshipone.odyssey.viewmodels.ArtistViewModel;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

public class ArtistsFragment extends OdysseyFragment<ArtistModel> implements AdapterView.OnItemClickListener {
    public static final String TAG = ArtistsFragment.class.getSimpleName();
    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    public static ArtistsFragment newInstance() {
        return new ArtistsFragment();
    }

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

        mAdapter = new ArtistsAdapter(getActivity(), useList);

        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(new ScrollSpeedListener(mAdapter));
        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) rootView.findViewById(R.id.empty_view_message)).setText(R.string.empty_artists_message);

        // register for context menu
        registerForContextMenu(mListView);

        // setup observer for the live data
        getViewModel().getData().observe(getViewLifecycleOwner(), this::onDataReady);

        return rootView;
    }

    @Override
    GenericViewModel<ArtistModel> getViewModel() {
        return new ViewModelProvider(this, new ArtistViewModel.ArtistViewModelFactory(getActivity().getApplication())).get(ArtistViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();

        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewArtistImageListener((ArtistsAdapter) mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewArtistImageListener((ArtistsAdapter) mAdapter);
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
            mArtistSelectedCallback = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    /**
     * Callback when an item in the ListView was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // identify current artist
        ArtistModel currentArtist = mAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        // If no artist ID is available get one (it is probably missing because of which method was used
        // to query artists. AlbumArtists vs. Artists MediaStore table.
        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        Bitmap bitmap = null;

        // Check if correct view type, to be safe
        if (view instanceof GenericImageViewItem) {
            bitmap = ((GenericImageViewItem) view).getBitmap();
        }

        // send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(new ArtistModel(artist, artistID), bitmap);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artists_fragment, menu);
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
            case R.id.fragment_artist_action_enqueue:
                enqueueArtist(info.position);
                return true;
            case R.id.fragment_artist_action_play:
                playArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Call the PBS to enqueue the selected artist
     *
     * @param position the position of the selected artist in the adapter
     */
    private void enqueueArtist(int position) {

        // identify current artist
        ArtistModel currentArtist = mAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String orderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));

        // enqueue artist
        try {
            ((GenericActivity) getActivity()).getPlaybackService().enqueueArtist(artistID, orderKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play the selected artist.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected artist in the adapter
     */
    private void playArtist(int position) {

        // identify current artist
        ArtistModel currentArtist = mAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String orderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));

        // enqueue artist
        try {
            ((GenericActivity) getActivity()).getPlaybackService().playArtist(artistID, orderKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
