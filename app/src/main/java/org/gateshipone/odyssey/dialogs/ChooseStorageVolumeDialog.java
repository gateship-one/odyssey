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

package org.gateshipone.odyssey.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;

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
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mDirectorySelectedCallback = (OnDirectorySelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnDirectorySelectedListener");
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.dialog_choose_storage_volume_title);
        mStorageVolumesAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, storageVolumes);
        builder.setAdapter(mStorageVolumesAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int item) {
                mDirectorySelectedCallback.onDirectorySelected(mStorageVolumesAdapter.getItem(item), true);
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
