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

package org.gateshipone.odyssey.loaders;

import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.playbackservice.statemanager.OdysseyDatabaseManager;

import java.util.ArrayList;
import java.util.List;

public class BookmarkLoader extends AsyncTaskLoader<List<BookmarkModel>> {

    /**
     * Flag if a header element should be inserted.
     */
    private final boolean mAddHeader;

    public BookmarkLoader(final Context context, final boolean addHeader) {
        super(context);
        mAddHeader = addHeader;
    }

    /**
     * Load all bookmarks from the database.
     */
    @Override
    public List<BookmarkModel> loadInBackground() {
        final Context context = getContext();

        List<BookmarkModel> bookmarks = new ArrayList<>();

        if (mAddHeader) {
            // add a dummy bookmark for the choose bookmark dialog
            // this bookmark represents the action to create a new bookmark in the dialog
            bookmarks.add(new BookmarkModel(-1, context.getString(R.string.create_new_bookmark), -1));
        }
        bookmarks.addAll(new OdysseyDatabaseManager(context).getBookmarks());

        return bookmarks;
    }

    /**
     * Start loading the data.
     * A previous load dataset will be ignored
     */
    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    /**
     * Stop the loader and cancel the current task.
     */
    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
