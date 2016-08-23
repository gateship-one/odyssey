/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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
import android.support.v4.content.AsyncTaskLoader;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.playbackservice.statemanager.OdysseyDatabaseManager;

import java.util.ArrayList;
import java.util.List;

public class BookmarkLoader extends AsyncTaskLoader<List<BookmarkModel>> {
    private final Context mContext;

    /**
     * Flag if a header element should be inserted.
     */
    private final boolean mAddHeader;

    public BookmarkLoader(Context context, boolean addHeader) {
        super(context);

        mContext = context;
        mAddHeader = addHeader;
    }

    /**
     * Load all bookmarks from the database.
     */
    @Override
    public List<BookmarkModel> loadInBackground() {
        List<BookmarkModel> bookmarks = new ArrayList<>();

        if (mAddHeader) {
            // add a dummy bookmark for the choose bookmark dialog
            // this bookmark represents the action to create a new bookmark in the dialog
            bookmarks.add(new BookmarkModel(-1, mContext.getString(R.string.create_new_bookmark), -1));
        }
        bookmarks.addAll(new OdysseyDatabaseManager(mContext).getBookmarks());

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
