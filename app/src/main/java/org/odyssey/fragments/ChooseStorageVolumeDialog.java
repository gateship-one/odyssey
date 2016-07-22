package org.odyssey.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;

import org.odyssey.R;
import org.odyssey.listener.OnDirectorySelectedListener;
import org.odyssey.utils.FileExplorerHelper;

import java.util.List;

public class ChooseStorageVolumeDialog extends DialogFragment {

    private OnDirectorySelectedListener mDirectorySelectedCallback;
    private ArrayAdapter<String> mStorageVolumesAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mDirectorySelectedCallback = (OnDirectorySelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnDirectorySelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // get the list of available storage volumes
        List<String> storageVolumes = FileExplorerHelper.getInstance(getContext()).getStorageVolumes();

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
