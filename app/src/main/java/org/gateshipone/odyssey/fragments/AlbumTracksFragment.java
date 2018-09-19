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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.adapter.TracksRecyclerViewAdapter;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.listener.OnArtistSelectedListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.loaders.TrackLoader;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.PreferenceHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewitems.GenericViewItemHolder;
import org.gateshipone.odyssey.viewmodels.AlbumViewModel;
import org.gateshipone.odyssey.viewmodels.TrackViewModel;
import org.gateshipone.odyssey.views.OdysseyRecyclerView;

import java.util.List;

public class AlbumTracksFragment extends OdysseyRecyclerFragment<TrackModel, GenericViewItemHolder> implements CoverBitmapLoader.CoverBitmapReceiver, ArtworkManager.onNewAlbumImageListener, OdysseyRecyclerView.OnItemClickListener {
    private static final String TAG = AlbumTracksFragment.class.getSimpleName();
    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    /**
     * Key values for arguments of the fragment
     */
    private final static String ARG_ALBUMMODEL = "albummodel";

    private final static String ARG_BITMAP = "bitmap";

    /**
     * The information of the displayed album
     */
    private AlbumModel mAlbum;

    private CoverBitmapLoader mBitmapLoader;

    private Bitmap mBitmap = null;

    private boolean mHideArtwork;

    /**
     * Action to execute when the user selects an item in the list
     */
    private PreferenceHelper.LIBRARY_TRACK_CLICK_ACTION mClickAction;

    public static AlbumTracksFragment newInstance(@NonNull final AlbumModel albumModel, @Nullable final Bitmap bitmap) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ALBUMMODEL, albumModel);
        if (bitmap != null) {
            args.putParcelable(ARG_BITMAP, bitmap);
        }

        final AlbumTracksFragment fragment = new AlbumTracksFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.recycler_list, container, false);

        mRecyclerAdapter = new TracksRecyclerViewAdapter(false);

        // get listview
        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.addOnItemClicklistener(this);

        setLinearLayoutManagerAndDecoration();

        registerForContextMenu(mRecyclerView);

        // get empty view
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) rootView.findViewById(R.id.empty_view_message)).setText(R.string.empty_tracks_message);

        // set up toolbar
        Bundle args = getArguments();

        mAlbum = args.getParcelable(ARG_ALBUMMODEL);
        mBitmap = args.getParcelable(ARG_BITMAP);

        setHasOptionsMenu(true);

        mBitmapLoader = new CoverBitmapLoader(getContext(), this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mHideArtwork = sharedPreferences.getBoolean(getContext().getString(R.string.pref_hide_artwork_key), getContext().getResources().getBoolean(R.bool.pref_hide_artwork_default));
        mClickAction = PreferenceHelper.getClickAction(sharedPreferences, getContext());

        final TrackViewModel model = ViewModelProviders.of(this, new TrackViewModel.TrackViewModelFactory(getActivity().getApplication(), mAlbum.getAlbumKey())).get(TrackViewModel.class);
        model.getData()
                .observe(this, this::onDataReady);

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
            // set up play button
            mToolbarAndFABCallback.setupFAB(v -> playAlbum(0));
        }

        if (!mHideArtwork && mBitmap == null) {
            mToolbarAndFABCallback.setupToolbar(mAlbum.getAlbumName(), false, false, false);
            final View rootView = getView();
            if (rootView != null) {
                getView().post(() -> {
                    int width = rootView.getMeasuredWidth();
                    mBitmapLoader.getAlbumImage(mAlbum, width, width);
                });
            }
        } else if (!mHideArtwork) {
            // Reuse image
            mToolbarAndFABCallback.setupToolbar(mAlbum.getAlbumName(), false, false, true);
            mToolbarAndFABCallback.setupToolbarImage(mBitmap);
            final View rootView = getView();
            if (rootView != null) {
                getView().post(() -> {
                    int width = rootView.getMeasuredWidth();
                    // Image too small
                    if (mBitmap.getWidth() < width) {
                        mBitmapLoader.getAlbumImage(mAlbum, width, width);
                    }
                });
            }
        } else {
            mToolbarAndFABCallback.setupToolbar(mAlbum.getAlbumName(), false, false, false);
        }

        ArtworkManager.getInstance(getContext()).registerOnNewAlbumImageListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(getContext()).unregisterOnNewAlbumImageListener(this);
    }

    /**
     * Play the album from the current position.
     */
    @Override
    public void onItemClick(int position) {
        switch (mClickAction) {
            case ACTION_ADD_SONG:
                enqueueTrack(position, false);
                break;
            case ACTION_PLAY_SONG:
                playTrack(position);
                break;
            case ACTION_PLAY_SONG_NEXT:
                enqueueTrack(position, true);
                break;
            case ACTION_CLEAR_AND_PLAY:
                playAlbum(position);
                break;
        }
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
        OdysseyRecyclerView.RecyclerViewContextMenuInfo info =
                (OdysseyRecyclerView.RecyclerViewContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_album_tracks_action_play:
                playTrack(info.position);
                return true;
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
            case R.id.action_reset_artwork:
                mToolbarAndFABCallback.setupToolbar(mAlbum.getAlbumName(), false, false, false);
                ArtworkManager.getInstance(getContext()).resetAlbumImage(mAlbum, getContext());
                return true;
            case R.id.action_add_album:
                enqueueAlbum();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        getArguments().remove(ARG_BITMAP);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Open a fragment for the artist of the selected album.
     *
     * @param position the position of the selected album in the adapter
     */
    private void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = mRecyclerAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, getActivity());

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(new ArtistModel(artistTitle, artistID), null);
    }

    /**
     * Call the PBS to enqueue the selected track.
     *
     * @param position the position of the selected track in the adapter
     * @param asNext   flag if the track should be enqueued as next
     */
    private void enqueueTrack(int position, boolean asNext) {

        TrackModel track = mRecyclerAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().enqueueTrack(track, asNext);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the selected track and then play it.
     *
     * @param position the position of the selected track in the adapter
     */
    private void playTrack(int position) {
        TrackModel track = mRecyclerAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().playTrack(track, false);
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
            ((GenericActivity) getActivity()).getPlaybackService().enqueueAlbum(mAlbum.getAlbumKey());
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
            ((GenericActivity) getActivity()).getPlaybackService().playAlbum(mAlbum.getAlbumKey(), position);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public void receiveAlbumBitmap(final Bitmap bm) {
        if (bm != null && mToolbarAndFABCallback != null) {
            getActivity().runOnUiThread(() -> {
                // set toolbar behaviour and title
                mToolbarAndFABCallback.setupToolbar(mAlbum.getAlbumName(), false, false, true);
                // set toolbar image
                mToolbarAndFABCallback.setupToolbarImage(bm);
                getArguments().putParcelable(ARG_BITMAP, bm);
            });
        }
    }

    @Override
    public void receiveArtistBitmap(final Bitmap bm) {

    }

    @Override
    public void newAlbumImage(AlbumModel album) {
        if (album.equals(mAlbum)) {
            if (!mHideArtwork) {
                int width = getView().getMeasuredWidth();
                mBitmapLoader.getAlbumImage(mAlbum, width, width);
            }
        }
    }
}
