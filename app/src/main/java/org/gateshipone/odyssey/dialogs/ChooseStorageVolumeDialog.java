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

package org.gateshipone.odyssey.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.listener.OnDirectorySelectedListener;
import org.gateshipone.odyssey.utils.FileExplorerHelper;

import java.util.List;

public class ChooseStorageVolumeDialog extends DialogFragment {

    /**
     * Listener to choose the storage volume
     */
    private OnDirectorySelectedListener mDirectorySelectedCallback;

    /**
     * Adapter used for the list of available storage volumes
     */
    private ArrayAdapter<String> mStorageVolumesAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mDirectorySelectedCallback = (OnDirectorySelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnDirectorySelectedListener");
        }
    }

    /**
     * Create the dialog to choose the current visible storage volume in the FilesFragment.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // get the list of available storage volumes
        List<String> storageVolumes = FileExplorerHelper.getInstance().getStorageVolumes(getContext());

        // Use the Builder class for convenient dialog construction
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());

        builder.setTitle(R.string.dialog_choose_storage_volume_title);
        mStorageVolumesAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, storageVolumes);
        builder.setAdapter(mStorageVolumesAdapter, (dialogInterface, item) -> mDirectorySelectedCallback.onDirectorySelected(mStorageVolumesAdapter.getItem(item), true));

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
