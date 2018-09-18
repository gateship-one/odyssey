/*
 * Copyright (C) 2018 Team Team Gateship-One
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

package org.gateshipone.odyssey.viewmodels;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.playbackservice.statemanager.OdysseyDatabaseManager;

import java.util.ArrayList;
import java.util.List;

public class BookmarkViewModel extends GenericViewModel<BookmarkModel> {

    /**
     * Flag if a header element should be inserted.
     */
    private final boolean mAddHeader;

    public BookmarkViewModel(@NonNull final Application application, final boolean addHeader) {
        super(application);

        mAddHeader = addHeader;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    void loadData() {
        new AsyncTask<Void, Void, List<BookmarkModel>>() {

            @Override
            protected List<BookmarkModel> doInBackground(Void... voids) {
                final Application application = getApplication();

                List<BookmarkModel> bookmarks = new ArrayList<>();

                if (mAddHeader) {
                    // add a dummy bookmark for the choose bookmark dialog
                    // this bookmark represents the action to create a new bookmark in the dialog
                    bookmarks.add(new BookmarkModel(-1, application.getString(R.string.create_new_bookmark), -1));
                }
                bookmarks.addAll(new OdysseyDatabaseManager(application).getBookmarks());

                return bookmarks;
            }

            @Override
            protected void onPostExecute(List<BookmarkModel> result) {
                setData(result);
            }

        }.execute();
    }
}
