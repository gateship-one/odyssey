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
import android.os.Bundle;

import org.gateshipone.odyssey.R;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ErrorDialog extends DialogFragment {

    private final static String ARG_ERRORDIALOGTITLE = "errordialogtitle";

    private final static String ARG_ERRORDIALOGMESSAGE = "errordialogmessage";

    /**
     * Returns a new ErrorDialog with title and message id as arguments
     */
    public static ErrorDialog newInstance(@StringRes final int title, @StringRes final int message) {
        final Bundle args = new Bundle();
        args.putInt(ErrorDialog.ARG_ERRORDIALOGTITLE, title);
        args.putInt(ErrorDialog.ARG_ERRORDIALOGMESSAGE, message);

        final ErrorDialog fragment = new ErrorDialog();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create the dialog to show an occured error
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // read arguments to identify the error title and message
        Bundle mArgs = requireArguments();

        int titleId = mArgs.getInt(ARG_ERRORDIALOGTITLE, -1);
        int messageId = mArgs.getInt(ARG_ERRORDIALOGMESSAGE, -1);

        String dialogTitle = (titleId == -1) ? "" : getString(titleId);
        String dialogMessage = (messageId == -1) ? "" : getString(messageId);

        builder.setTitle(dialogTitle).setMessage(dialogMessage)
                .setNegativeButton(R.string.error_dialog_ok_action, (dialog, id) -> dismiss());

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
