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

import static org.gateshipone.odyssey.artwork.BulkDownloadService.BUNDLE_KEY_ALBUM_PROVIDER;
import static org.gateshipone.odyssey.artwork.BulkDownloadService.BUNDLE_KEY_ARTIST_PROVIDER;
import static org.gateshipone.odyssey.artwork.BulkDownloadService.BUNDLE_KEY_WIFI_ONLY;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artwork.BulkDownloadService;

public class BulkDownloaderDialog extends DialogFragment {

    private static final String ARG_DIALOGTITLE = "dialogtitle";

    private static final String ARG_DIALOGMESSAGE = "dialogmessage";

    private static final String ARG_POSITIVEMESSAGE = "positivemessage";

    /**
     * Create a new BulkDownloaderDialog instance and set the given arguments
     *
     * @param title          the string resource id for the dialog title
     * @param message        the string resource id for the dialog message
     * @param positiveButton the string resource id for the positive button title
     * @return a new BulkDownloaderDialog instance
     */
    public static BulkDownloaderDialog newInstance(@StringRes final int title, @StringRes final int message, @StringRes final int positiveButton) {
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOGTITLE, title);
        args.putInt(ARG_DIALOGMESSAGE, message);
        args.putInt(ARG_POSITIVEMESSAGE, positiveButton);

        BulkDownloaderDialog fragment = new BulkDownloaderDialog();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create the dialog to show the info dialog before starting the bulkdownload
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());

        // read arguments to identify the error title and message
        Bundle mArgs = requireArguments();

        int titleId = mArgs.getInt(ARG_DIALOGTITLE, -1);
        int messageId = mArgs.getInt(ARG_DIALOGMESSAGE, -1);
        int positiveButtonId = mArgs.getInt(ARG_POSITIVEMESSAGE, -1);

        String dialogTitle = (titleId == -1) ? "" : getString(titleId);
        String dialogMessage = (messageId == -1) ? "" : getString(messageId);

        builder.setTitle(dialogTitle).setMessage(dialogMessage)
                .setPositiveButton(positiveButtonId, (dialog, id) -> {
                    // start the bulk download service
                    Intent serviceIntent = new Intent(requireActivity(), BulkDownloadService.class);
                    serviceIntent.setAction(BulkDownloadService.ACTION_START_BULKDOWNLOAD);

                    // get the current settings for the bulk download
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                    serviceIntent.putExtra(BUNDLE_KEY_ARTIST_PROVIDER, sharedPref.getString(getString(R.string.pref_artist_provider_key),
                            getString(R.string.pref_artwork_provider_artist_default)));
                    serviceIntent.putExtra(BUNDLE_KEY_ALBUM_PROVIDER, sharedPref.getString(getString(R.string.pref_album_provider_key),
                            getString(R.string.pref_artwork_provider_album_default)));
                    serviceIntent.putExtra(BUNDLE_KEY_WIFI_ONLY, sharedPref.getBoolean(getString(R.string.pref_download_wifi_only_key),
                            getResources().getBoolean(R.bool.pref_download_wifi_default)));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requireActivity().startForegroundService(serviceIntent);
                    } else {
                        requireActivity().startService(serviceIntent);
                    }
                });

        return builder.create();
    }
}
