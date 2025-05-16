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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.adapter.AlbumsRecyclerViewAdapter;
import org.gateshipone.odyssey.artwork.ArtworkManager;
import org.gateshipone.odyssey.listener.OnAlbumSelectedListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;
import org.gateshipone.odyssey.utils.RecyclerScrollSpeedListener;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewitems.GenericImageViewItem;
import org.gateshipone.odyssey.viewitems.GenericViewItemHolder;
import org.gateshipone.odyssey.viewmodels.AlbumViewModel;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;
import org.gateshipone.odyssey.views.OdysseyRecyclerView;

import java.util.List;

public class ArtistAlbumsFragment extends OdysseyRecyclerFragment<AlbumModel, GenericViewItemHolder> implements CoverBitmapLoader.CoverBitmapReceiver, ArtworkManager.onNewArtistImageListener, OdysseyRecyclerView.OnItemClickListener {
    private static final String TAG = ArtistAlbumsFragment.class.getSimpleName();
    /**
     * {@link ArtistModel} to show albums for
     */
    private ArtistModel mArtist;

    /**
     * key values for arguments of the fragment
     */
    private static final String ARG_ARTISTMODEL = "artistmodel";

    private static final String ARG_BITMAP = "bitmap";

    private CoverBitmapLoader mBitmapLoader;

    private Bitmap mBitmap;

    private boolean mHideArtwork;

    /**
     * Listener to open an album
     */
    protected OnAlbumSelectedListener mAlbumSelectedCallback;

    /**
     * Save the last scroll position to resume there
     */
    protected int mLastPosition = -1;

    public static ArtistAlbumsFragment newInstance(@NonNull final ArtistModel artistModel, @Nullable final Bitmap bitmap) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ARTISTMODEL, artistModel);
        if (bitmap != null) {
            args.putParcelable(ARG_BITMAP, bitmap);
        }

        final ArtistAlbumsFragment fragment = new ArtistAlbumsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_list_refresh, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String viewAppearance = sharedPref.getString(getString(R.string.pref_view_library_key), getString(R.string.pref_library_view_default));
        mHideArtwork = sharedPref.getBoolean(requireContext().getString(R.string.pref_hide_artwork_key), requireContext().getResources().getBoolean(R.bool.pref_hide_artwork_default));

        // get swipe layout
        mSwipeRefreshLayout = view.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(requireContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(requireContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshContent);

        mRecyclerView = view.findViewById(R.id.recycler_view);

        final boolean useList = viewAppearance.equals(getString(R.string.pref_library_view_list_key));

        mRecyclerAdapter = new AlbumsRecyclerViewAdapter(getContext(), useList);

        if (useList) {
            mRecyclerView.setAdapter(mRecyclerAdapter);

            setLinearLayoutManagerAndDecoration();
        } else {
            mRecyclerView.setAdapter(mRecyclerAdapter);

            setGridLayoutManagerAndDecoration();
        }

        mRecyclerView.addOnScrollListener(new RecyclerScrollSpeedListener(mRecyclerAdapter));
        mRecyclerView.addOnItemClicklistener(this);

        registerForContextMenu(mRecyclerView);

        // get empty view
        mEmptyView = view.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) view.findViewById(R.id.empty_view_message)).setText(R.string.empty_albums_message);

        // read arguments
        Bundle args = requireArguments();
        mArtist = args.getParcelable(ARG_ARTISTMODEL);
        mBitmap = args.getParcelable(ARG_BITMAP);

        setHasOptionsMenu(true);

        mBitmapLoader = new CoverBitmapLoader(requireContext(), this);

        // setup observer for the live data
        getViewModel().getData().observe(getViewLifecycleOwner(), this::onDataReady);
    }

    @Override
    GenericViewModel<AlbumModel> getViewModel() {
        return new ViewModelProvider(this, new AlbumViewModel.AlbumViewModelFactory(requireActivity().getApplication(), mArtist.getArtistID())).get(AlbumViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();

        ArtworkManager.getInstance(getContext()).registerOnNewAlbumImageListener((AlbumsRecyclerViewAdapter) mRecyclerAdapter);

        if (mToolbarAndFABCallback != null) {
            // set up play button
            mToolbarAndFABCallback.setupFAB(v -> playArtist());

            // set toolbar behaviour and title
            if (!mHideArtwork && mBitmap == null) {
                mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
                final View rootView = getView();
                if (rootView != null) {
                    getView().post(() -> {
                        int width = rootView.getWidth();
                        mBitmapLoader.getArtistImage(mArtist, width, width);
                    });
                }

            } else if (!mHideArtwork) {
                mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, true);
                mToolbarAndFABCallback.setupToolbarImage(mBitmap);
                final View rootView = getView();
                if (rootView != null) {
                    getView().post(() -> {
                        int width = rootView.getWidth();

                        // Image too small
                        if (mBitmap.getWidth() < width) {
                            mBitmapLoader.getArtistImage(mArtist, width, width);
                        }
                    });
                }
            } else {
                mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
            }
        }

        ArtworkManager.getInstance(getContext()).registerOnNewArtistImageListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(getContext()).unregisterOnNewAlbumImageListener((AlbumsRecyclerViewAdapter) mRecyclerAdapter);

        ArtworkManager.getInstance(getContext()).unregisterOnNewArtistImageListener(this);
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
            throw new ClassCastException(context + " must implement OnAlbumSelectedListener");
        }

        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement ToolbarAndFABCallback");
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
            mRecyclerView.getLayoutManager().scrollToPosition(mLastPosition);
            mLastPosition = -1;
        }
    }

    /**
     * Callback when an item in the GridView was clicked.
     */
    @Override
    public void onItemClick(int position) {
        // save last scroll position
        mLastPosition = position;

        // identify current album
        AlbumModel currentAlbum = mRecyclerAdapter.getItem(position);

        Bitmap bitmap = null;

        final View view = mRecyclerView.getChildAt(position);

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
     * @param asNext
     */
    protected void enqueueAlbum(int position, boolean asNext) {
        // identify current album
        AlbumModel clickedAlbum = mRecyclerAdapter.getItem(position);
        long albumId = clickedAlbum.getAlbumId();

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String trackOrderKey = sharedPref.getString(getString(R.string.pref_album_tracks_sort_order_key), getString(R.string.pref_album_tracks_sort_default));

        // enqueue album
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().enqueueAlbum(albumId, trackOrderKey, asNext);
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
        AlbumModel clickedAlbum = mRecyclerAdapter.getItem(position);
        long albumId = clickedAlbum.getAlbumId();

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String trackOrderKey = sharedPref.getString(getString(R.string.pref_album_tracks_sort_order_key), getString(R.string.pref_album_tracks_sort_default));

        // play album
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().playAlbum(albumId, trackOrderKey, 0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artist_albums_fragment, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        OdysseyRecyclerView.RecyclerViewContextMenuInfo info =
                (OdysseyRecyclerView.RecyclerViewContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        final int itemId = item.getItemId();
        if (itemId == R.id.fragment_artist_albums_action_enqueue) {
            enqueueAlbum(info.position, false);
            return true;

        } else if (itemId == R.id.fragment_artist_albums_action_enqueueasnext) {
            enqueueAlbum(info.position, true);
            return true;
        }
        else if (itemId == R.id.fragment_artist_albums_action_play) {
            playAlbum(info.position);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_artist_albums_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(requireContext(), R.attr.app_color_on_surface);

        Drawable drawable = menu.findItem(R.id.action_add_artist_albums).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_artist_albums).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.action_reset_artwork) {
            mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
            ArtworkManager.getInstance(getContext()).resetImage(mArtist);
            return true;
        } else if (itemId == R.id.action_add_artist_albums) {
            enqueueArtist();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        requireArguments().remove(ARG_BITMAP);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Call the PBS to enqueue artist.
     */
    private void enqueueArtist() {
        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String albumOrderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));
        String trackOrderKey = sharedPref.getString(getString(R.string.pref_album_tracks_sort_order_key), getString(R.string.pref_album_tracks_sort_default));

        // enqueue artist
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().enqueueArtist(mArtist.getArtistID(), albumOrderKey, trackOrderKey, false);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play artist.
     * A previous playlist will be cleared.
     */
    private void playArtist() {
        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String albumOrderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));
        String trackOrderKey = sharedPref.getString(getString(R.string.pref_album_tracks_sort_order_key), getString(R.string.pref_album_tracks_sort_default));

        // play artist
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().playArtist(mArtist.getArtistID(), albumOrderKey, trackOrderKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void receiveArtistBitmap(final Bitmap bm) {
        if (bm != null && mToolbarAndFABCallback != null) {
            requireActivity().runOnUiThread(() -> {
                // set toolbar behaviour and title
                mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, true);
                // set toolbar image
                mToolbarAndFABCallback.setupToolbarImage(bm);
                requireArguments().putParcelable(ARG_BITMAP, bm);
            });
        }
    }

    @Override
    public void receiveAlbumBitmap(final Bitmap bm) {

    }

    @Override
    public void newArtistImage(ArtistModel artist) {
        if (artist.equals(mArtist)) {
            if (!mHideArtwork) {
                int width = requireView().getWidth();
                mBitmapLoader.getArtistImage(mArtist, width, width);
            }
        }
    }
}
