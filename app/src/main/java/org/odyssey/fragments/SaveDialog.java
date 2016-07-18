package org.odyssey.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.EditText;

import org.odyssey.R;
import org.odyssey.listener.OnSaveDialogListener;

public class SaveDialog extends DialogFragment {

    public final static String ARG_OBJECTTYPE = "objecttype";

    public enum OBJECTTYPE {
        PLAYLIST, BOOKMARK
    }

    OnSaveDialogListener mSaveCallback;

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSaveCallback = (OnSaveDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSaveDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // read arguments to identify type of the object which should be saved
        Bundle mArgs = getArguments();
        final OBJECTTYPE type = (OBJECTTYPE) mArgs.get(ARG_OBJECTTYPE);

        String dialogTitle = "";
        String editTextDefaultTitle = "";

        if (type != null) {
            switch (type) {
                case PLAYLIST:
                    dialogTitle = getString(R.string.dialog_save_playlist);
                    editTextDefaultTitle = getString(R.string.default_playlist_title);
                    break;
                case BOOKMARK:
                    dialogTitle = getString(R.string.dialog_create_bookmark);
                    editTextDefaultTitle = getString(R.string.default_bookmark_title);
                    break;
            }
        }

        // create edit text for title
        final EditText editTextTitle = new EditText(getActivity());
        editTextTitle.setText(editTextDefaultTitle);
        builder.setView(editTextTitle);

        builder.setMessage(dialogTitle).setPositiveButton(R.string.dialog_action_save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // accept title and call callback method
                String objectTitle = editTextTitle.getText().toString();
                mSaveCallback.onSaveObject(objectTitle, type);
            }
        }).setNegativeButton(R.string.dialog_action_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog dont save object
                getDialog().cancel();
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
