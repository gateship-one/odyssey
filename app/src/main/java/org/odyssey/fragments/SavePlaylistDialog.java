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
import org.odyssey.listener.OnSavePlaylistListener;

public class SavePlaylistDialog extends DialogFragment {

    private OnSavePlaylistListener mSavePlaylistCallback;

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSavePlaylistCallback = (OnSavePlaylistListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPlaylistNameListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText editTextPlaylistName = new EditText(getActivity());
        editTextPlaylistName.setText(R.string.default_playlist_title);
        builder.setView(editTextPlaylistName);

        builder.setMessage(R.string.dialog_save_playlist).setPositiveButton(R.string.dialog_action_save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // accept playlist name
                String playlistName = editTextPlaylistName.getText().toString();
                mSavePlaylistCallback.onSavePlaylist(playlistName);
            }
        }).setNegativeButton(R.string.dialog_action_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog dont create playlist
                getDialog().cancel();
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}