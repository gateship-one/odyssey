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

import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.BookmarksAdapter;
import org.gateshipone.odyssey.listener.OnSaveDialogListener;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.BookmarkViewModel;

import java.util.List;

public class ChooseBookmarkDialog extends DialogFragment {

    /**
     * Listener to save the bookmark
     */
    OnSaveDialogListener mSaveCallback;

    /**
     * Adapter used for the ListView
     */
    private BookmarksAdapter mBookmarksAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSaveCallback = (OnSaveDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnSaveDialogListener");
        }
    }

    /**
     * Create the dialog to choose to override an existing bookmark or to create a new bookmark.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mBookmarksAdapter = new BookmarksAdapter(getActivity());

        builder.setTitle(R.string.dialog_choose_bookmark).setAdapter(mBookmarksAdapter, (dialog, which) -> {

            if (which == 0) {
                // open save dialog to create a new bookmark
                SaveDialog saveDialog = SaveDialog.newInstance(SaveDialog.OBJECTTYPE.BOOKMARK);
                saveDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "SaveDialog");
            } else {
                // override existing bookmark
                BookmarkModel bookmark = mBookmarksAdapter.getItem(which);
                String objectTitle = bookmark.getTitle();
                mSaveCallback.onSaveObject(objectTitle, SaveDialog.OBJECTTYPE.BOOKMARK);
            }
        }).setNegativeButton(R.string.dialog_action_cancel, (dialog, id) -> {
            // User cancelled the dialog dont save object
            getDialog().cancel();
        });

        final BookmarkViewModel model = ViewModelProviders.of(this, new BookmarkViewModel.BookmarkViewModelFactory(getActivity().getApplication(), true)).get(BookmarkViewModel.class);
        model.getData()
                .observe(this, data -> mBookmarksAdapter.swapModel(data));

        // set divider
        AlertDialog dlg = builder.create();
        dlg.getListView().setDivider(new ColorDrawable(ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_background_selected)));
        dlg.getListView().setDividerHeight(getResources().getDimensionPixelSize(R.dimen.list_divider_size));

        return dlg;
    }
}
