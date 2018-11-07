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
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.BookmarksAdapter;
import org.gateshipone.odyssey.listener.OnSaveDialogListener;
import org.gateshipone.odyssey.loaders.BookmarkLoader;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.utils.ThemeUtils;

import java.util.List;

public class ChooseBookmarkDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<List<BookmarkModel>> {

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
     * This method creates a new loader for this fragment.
     *
     * @param id   The id of the loader
     * @param args Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<BookmarkModel>> onCreateLoader(int id, Bundle args) {
        return new BookmarkLoader(getActivity(), true);
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param data   Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<BookmarkModel>> loader, List<BookmarkModel> data) {
        mBookmarksAdapter.swapModel(data);
    }

    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<BookmarkModel>> loader) {
        mBookmarksAdapter.swapModel(null);
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

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

        // set divider
        AlertDialog dlg = builder.create();
        dlg.getListView().setDivider(new ColorDrawable(ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_background_selected)));
        dlg.getListView().setDividerHeight(getResources().getDimensionPixelSize(R.dimen.list_divider_size));

        return dlg;
    }
}
