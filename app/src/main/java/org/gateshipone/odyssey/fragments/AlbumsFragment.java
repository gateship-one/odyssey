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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.listener.OnArtistSelectedListener;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.viewmodels.AlbumViewModel;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;

import java.util.List;

public class AlbumsFragment extends GenericAlbumsFragment {

    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    public static AlbumsFragment newInstance() {
        return new AlbumsFragment();
    }

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = super.onCreateView(inflater, container, savedInstanceState);

        final AlbumViewModel model = (AlbumViewModel) getViewModel();
        model.getData()
                .observe(this, this::onDataReady);

        return rootView;
    }

    @Override
    GenericViewModel<AlbumModel> getViewModel() {
        return ViewModelProviders.of(this, new AlbumViewModel.AlbumViewModelFactory(getActivity().getApplication(), false)).get(AlbumViewModel.class);
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
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_albums_fragment, menu);
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
            case R.id.fragment_albums_action_enqueue:
                enqueueAlbum(info.position);
                return true;
            case R.id.fragment_albums_action_play:
                playAlbum(info.position);
                return true;
            case R.id.fragment_albums_action_showartist:
                showArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Open a fragment for the artist of the selected album.
     *
     * @param position the position of the selected album in the adapter
     */
    private void showArtist(int position) {
        // identify current artist

        AlbumModel clickedAlbum = mAdapter.getItem(position);

        String artistTitle = clickedAlbum.getArtistName();
        long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, getActivity());

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(new ArtistModel(artistTitle, artistID), null);
    }
}
