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

package org.gateshipone.odyssey.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.SavedPlaylistsAdapter;
import org.gateshipone.odyssey.listener.OnSaveDialogListener;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.PlaylistViewModel;

public class ChoosePlaylistDialog extends DialogFragment {

    /**
     * Listener to save the bookmark
     */
    OnSaveDialogListener mSaveCallback;

    /**
     * Adapter used for the ListView
     */
    private SavedPlaylistsAdapter mPlaylistsListViewAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSaveCallback = (OnSaveDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnSaveDialogListener");
        }
    }

    /**
     * Create the dialog to choose to override an existing bookmark or to create a new bookmark.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());

        mPlaylistsListViewAdapter = new SavedPlaylistsAdapter(getActivity());

        builder
                .setTitle(R.string.dialog_choose_playlist)
                .setAdapter(mPlaylistsListViewAdapter, (dialog, which) -> {

                    if (which == 0) {
                        // open save dialog to create a new playlist
                        SaveDialog saveDialog = SaveDialog.newInstance(SaveDialog.OBJECTTYPE.PLAYLIST);
                        saveDialog.show(requireActivity().getSupportFragmentManager(), "SaveDialog");
                    } else {
                        // override existing playlist
                        PlaylistModel playlist = mPlaylistsListViewAdapter.getItem(which);
                        String objectTitle = playlist.getPlaylistName();
                        mSaveCallback.onSaveObject(objectTitle, SaveDialog.OBJECTTYPE.PLAYLIST);
                    }
                })
                .setNegativeButton(R.string.dialog_action_cancel, (dialog, id) -> {
                    // User cancelled the dialog dont save object
                    requireDialog().cancel();
                });

        // setup playlist ViewModel
        final PlaylistViewModel model = new ViewModelProvider(this, new PlaylistViewModel.PlaylistViewModelFactory(requireActivity().getApplication(), true, true))
                .get(PlaylistViewModel.class);
        model.getData()
                .observe(this, data -> mPlaylistsListViewAdapter.swapModel(data));
        model.reloadData();

        // set divider
        AlertDialog dlg = builder.create();
        dlg.getListView().setDivider(new ColorDrawable(ThemeUtils.getThemeColor(requireContext(), R.attr.odyssey_color_divider)));
        dlg.getListView().setDividerHeight(getResources().getDimensionPixelSize(R.dimen.list_divider_size));

        return dlg;
    }
}
