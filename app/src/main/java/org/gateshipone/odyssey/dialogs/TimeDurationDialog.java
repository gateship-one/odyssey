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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.listener.OnStartSleepTimerListener;

public class TimeDurationDialog extends DialogFragment {

    private static final String ARG_PRESET_DURATION = "objecttype";

    private static final String ARG_PRESET_STOP_AFTER_CURRENT = "stopaftercurrent";

    private OnStartSleepTimerListener mOnStartSleepTimerCallback;

    private NumberPicker mMinutesPicker;

    private NumberPicker mSecondsPicker;

    private CheckBox mStopAfterCurrentCheckBox;

    public static TimeDurationDialog newInstance(long presetDurationMS, boolean stopAfterCurrent) {
        final Bundle args = new Bundle();
        args.putLong(ARG_PRESET_DURATION, presetDurationMS);
        args.putBoolean(ARG_PRESET_STOP_AFTER_CURRENT, stopAfterCurrent);

        final TimeDurationDialog fragment = new TimeDurationDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mOnStartSleepTimerCallback = (OnStartSleepTimerListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnDirectorySelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View durationView = inflater.inflate(R.layout.duration_picker, null);

        mMinutesPicker = durationView.findViewById(R.id.duration_minutes_picker);

        mSecondsPicker = durationView.findViewById(R.id.duration_seconds_picker);

        mStopAfterCurrentCheckBox = durationView.findViewById(R.id.duration_stop_after_current_checkbox);

        final Bundle arguments = getArguments();
        setupPicker(arguments.getLong(ARG_PRESET_DURATION, 0));
        mStopAfterCurrentCheckBox.setChecked(arguments.getBoolean(ARG_PRESET_STOP_AFTER_CURRENT, false));

        builder.setView(durationView);

        builder.setTitle(R.string.dialog_sleep_timer);

        builder.setPositiveButton(R.string.dialog_sleep_timer_action_start, (dialog, which) -> {
            final long durationMS = getDuration();

            if (durationMS == 0) {
                // cancel the dialog if duration is still zero
                dialog.cancel();
            } else {
                final boolean stopAfterCurrent = getStopAfterCurrent();

                mOnStartSleepTimerCallback.onStartSleepTimer(durationMS, stopAfterCurrent);
            }
        });
        builder.setNegativeButton(R.string.dialog_action_cancel, (dialog, which) -> dialog.cancel());

        return builder.create();
    }

    private long getDuration() {
        final int minutes = mMinutesPicker.getValue();
        final int seconds = mSecondsPicker.getValue();

        return minutes * 60L * 1000L + seconds * 1000L;
    }

    private boolean getStopAfterCurrent() {
        return mStopAfterCurrentCheckBox.isChecked();
    }

    private void setupPicker(final long durationMS) {
        int seconds = (int) (durationMS / 1000);

        int minutes = seconds / 60;

        seconds = seconds % 60;

        mMinutesPicker.setMinValue(0);
        mMinutesPicker.setMaxValue(59);
        mMinutesPicker.setValue(minutes);
        mMinutesPicker.setWrapSelectorWheel(true);

        mSecondsPicker.setMinValue(0);
        mSecondsPicker.setMaxValue(59);
        mSecondsPicker.setValue(seconds);
        mSecondsPicker.setWrapSelectorWheel(true);
    }
}
