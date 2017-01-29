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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.loaders.AlbumLoader;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;
import org.gateshipone.odyssey.utils.ThemeUtils;

import java.util.List;

public class ArtistAlbumsFragment extends GenericAlbumsFragment implements CoverBitmapLoader.CoverBitmapListener, ArtworkManager.onNewArtistImageListener {
    /**
     * {@link ArtistModel} to show albums for
     */
    private ArtistModel mArtist;

    /**
     * key values for arguments of the fragment
     */
    // FIXME move to separate class to get unified constants?
    public final static String ARG_ARTISTMODEL = "artistmodel";

    private CoverBitmapLoader mBitmapLoader;

    private boolean mHideArtwork;

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        // read arguments
        Bundle args = getArguments();
        mArtist = args.getParcelable(ARG_ARTISTMODEL);

        setHasOptionsMenu(true);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mHideArtwork = sharedPreferences.getBoolean(getContext().getString(R.string.pref_hide_artwork_key), getContext().getResources().getBoolean(R.bool.pref_hide_artwork_default));

        mBitmapLoader = new CoverBitmapLoader(getContext(), this);

        return rootView;
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
            mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
            // set up play button
            mToolbarAndFABCallback.setupFAB(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playArtist();
                }
            });
        }

        if (!mHideArtwork) {
            mBitmapLoader.getArtistImage(mArtist);
        }

        ArtworkManager.getInstance(getContext()).registerOnNewArtistImageListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(getContext()).unregisterOnNewArtistImageListener(this);
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<AlbumModel>> onCreateLoader(int id, Bundle bundle) {
        return new AlbumLoader(getActivity(), mArtist.getArtistID());
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artist_albums_fragment, menu);
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
            case R.id.fragment_artist_albums_action_enqueue:
                enqueueAlbum(info.position);
                return true;
            case R.id.fragment_artist_albums_action_play:
                playAlbum(info.position);
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
        menuInflater.inflate(R.menu.options_menu_artist_albums_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset_artwork:
                mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
                ArtworkManager.getInstance(getContext()).resetArtistImage(mArtist, getContext());
                return true;
            case R.id.action_add_artist_albums:
                enqueueArtist();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Call the PBS to enqueue artist.
     */
    private void enqueueArtist() {
        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String orderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));

        // enqueue artist
        try {
            mServiceConnection.getPBS().enqueueArtist(mArtist.getArtistID(), orderKey);
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

        // play all album of current artist if exists

        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // enqueue artist
        enqueueArtist();

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void receiveBitmap(final Bitmap bm) {
        if (bm != null && mToolbarAndFABCallback != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // set toolbar behaviour and title
                    mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, true);
                    // set toolbar image
                    mToolbarAndFABCallback.setupToolbarImage(bm);
                }
            });
        }
    }

    @Override
    public void newArtistImage(ArtistModel artist) {
        if (artist.equals(mArtist)) {
            if (!mHideArtwork) {
                mBitmapLoader.getArtistImage(mArtist);
            }
        }
    }
}
