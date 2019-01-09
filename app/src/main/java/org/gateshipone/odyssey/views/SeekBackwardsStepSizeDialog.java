/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://gitlab.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.views;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;


import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.playbackservice.IOdysseyPlaybackService;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;


public class SeekBackwardsStepSizeDialog extends DialogFragment implements SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;

    private TextView mDialogLabel;

    private int mStepSize;

    PlaybackServiceConnection mPBSConnection;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.resume_step_size_dialog, container, false);

        mSeekBar = rootView.findViewById(R.id.volume_seekbar);
        mDialogLabel = rootView.findViewById(R.id.dialog_text);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        mStepSize = sharedPreferences.getInt(getString(R.string.pref_seek_backwards_key), getResources().getInteger(R.integer.pref_seek_backwards_default));

        mSeekBar.setProgress(mStepSize);
        mSeekBar.setOnSeekBarChangeListener(this);

        rootView.findViewById(R.id.button_ok).setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.pref_seek_backwards_key), mStepSize);
            editor.apply();
            try {
                mPBSConnection.getPBS().changeAutoBackwardsSeekAmount(mStepSize);
            } catch (RemoteException e) {

            }
            dismiss();
        });

        rootView.findViewById(R.id.button_cancel).setOnClickListener(v -> dismiss());

        updateLabels();

        mPBSConnection = new PlaybackServiceConnection(getContext());
        mPBSConnection.openConnection();

        return rootView;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mStepSize = progress;
        updateLabels();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateLabels() {
        mDialogLabel.setText(getString(R.string.preference_resume_step_size_dialog_title, mStepSize));
    }
}
