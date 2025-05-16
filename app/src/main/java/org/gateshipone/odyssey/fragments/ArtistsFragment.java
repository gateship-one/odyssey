/*
 * Copyright (C) 2023 Team Gateship-One
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

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
import org.gateshipone.odyssey.viewmodels.SearchViewModel;

public class ArtistsFragment extends OdysseyFragment<ArtistModel> implements AdapterView.OnItemClickListener {
    public static final String TAG = ArtistsFragment.class.getSimpleName();
    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    public static ArtistsFragment newInstance() {
        return new ArtistsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        final String viewAppearance = sharedPref.getString(getString(R.string.pref_view_library_key), getString(R.string.pref_library_view_default));

        final boolean useList = viewAppearance.equals(getString(R.string.pref_library_view_list_key));

        if (useList) {
            return inflater.inflate(R.layout.list_refresh, container, false);
        } else {
            return inflater.inflate(R.layout.grid_refresh, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        final String viewAppearance = sharedPref.getString(getString(R.string.pref_view_library_key), getString(R.string.pref_library_view_default));

        boolean useList = viewAppearance.equals(getString(R.string.pref_library_view_list_key));

        if (useList) {
            // get listview
            mListView = view.findViewById(R.id.list_refresh_listview);
        } else {
            // get gridview
            mListView = view.findViewById(R.id.grid_refresh_gridview);
        }

        // get swipe layout
        mSwipeRefreshLayout = view.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(requireContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(requireContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshContent);

        mAdapter = new ArtistsAdapter(getActivity(), useList);

        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(new ScrollSpeedListener(mAdapter));
        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = view.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) view.findViewById(R.id.empty_view_message)).setText(R.string.empty_artists_message);

        // register for context menu
        registerForContextMenu(mListView);

        // setup observer for the live data
        getViewModel().getData().observe(getViewLifecycleOwner(), this::onDataReady);

        final SearchViewModel searchViewModel = new ViewModelProvider(requireParentFragment()).get(SearchViewModel.class);
        searchViewModel.getSearchString().observe(getViewLifecycleOwner(), searchString -> {
            if (searchString != null) {
                applyFilter(searchString);
            } else {
                removeFilter();
            }
        });
    }

    @Override
    GenericViewModel<ArtistModel> getViewModel() {
        return new ViewModelProvider(this, new ArtistViewModel.ArtistViewModelFactory(requireActivity().getApplication())).get(ArtistViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();

        ArtworkManager.getInstance(getContext()).registerOnNewArtistImageListener((ArtistsAdapter) mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(getContext()).unregisterOnNewArtistImageListener((ArtistsAdapter) mAdapter);
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
            throw new ClassCastException(context + " must implement OnArtistSelectedListener");
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
        long artistId = currentArtist.getArtistID();

        // If no artist ID is available get one (it is probably missing because of which method was used
        // to query artists. AlbumArtists vs. Artists MediaStore table.
        if (artistId == -1) {
            // Try to get the artistId manually because it seems to be missing
            artistId = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        Bitmap bitmap = null;

        // Check if correct view type, to be safe
        if (view instanceof GenericImageViewItem) {
            bitmap = ((GenericImageViewItem) view).getBitmap();
        }

        // send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(new ArtistModel(artist, artistId), bitmap);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = requireActivity().getMenuInflater();
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

        final int itemId = item.getItemId();

        if (itemId == R.id.fragment_artist_action_enqueue) {
            enqueueArtist(info.position, false);
            return true;
        } else if (itemId == R.id.fragment_artist_action_enqueueasnext) {
            enqueueArtist(info.position, true);
            return true;
        } else if (itemId == R.id.fragment_artist_action_play) {
            playArtist(info.position);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Call the PBS to enqueue the selected artist
     *
     * @param position the position of the selected artist in the adapter
     * @param asNext
     */
    private void enqueueArtist(int position, boolean asNext) {

        // identify current artist
        ArtistModel currentArtist = mAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistId = currentArtist.getArtistID();

        if (artistId == -1) {
            // Try to get the artistId manually because it seems to be missing
            artistId = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String albumOrderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));
        String trackOrderKey = sharedPref.getString(getString(R.string.pref_album_tracks_sort_order_key), getString(R.string.pref_album_tracks_sort_default));

        // enqueue artist
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().enqueueArtist(artistId, albumOrderKey, trackOrderKey, asNext);
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
        long artistId = currentArtist.getArtistID();

        if (artistId == -1) {
            // Try to get the artistId manually because it seems to be missing
            artistId = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String albumOrderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));
        String trackOrderKey = sharedPref.getString(getString(R.string.pref_album_tracks_sort_order_key), getString(R.string.pref_album_tracks_sort_default));

        // enqueue artist
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().playArtist(artistId, albumOrderKey, trackOrderKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
