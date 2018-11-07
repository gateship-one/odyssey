/*
 * Copyright (C) 2018 Team Gateship-One
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

import androidx.fragment.app.DialogFragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.webkit.WebView;

import org.gateshipone.odyssey.R;

public class LicensesDialog extends DialogFragment {

    public static LicensesDialog newInstance() {
        return new LicensesDialog();
    }

    /**
     * Create the dialog to show the third party licenses in a webview
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        WebView view = new WebView(getActivity());
        view.loadUrl("file:///android_asset/thirdparty_licenses.html");
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.odyssey_thirdparty_licenses_dialog_title))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
