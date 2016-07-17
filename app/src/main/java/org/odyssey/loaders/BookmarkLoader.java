package org.odyssey.loaders;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;

import org.odyssey.models.BookmarkModel;
import org.odyssey.playbackservice.statemanager.CurrentPlaylistDBHelper;
import org.odyssey.playbackservice.statemanager.StateTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Frederik on 17.07.2016.
 */
public class BookmarkLoader extends AsyncTaskLoader<List<BookmarkModel>> {
    private final Context mContext;

    public BookmarkLoader(Context context) {
        super(context);

        mContext = context;
    }

    @Override
    public List<BookmarkModel> loadInBackground() {

        // TODO for the moment load all bookmarks from the statetable

        ArrayList<BookmarkModel> bookmarks = new ArrayList<>();

        SQLiteDatabase mBookmarkDB = new CurrentPlaylistDBHelper(mContext).getReadableDatabase();

        Cursor bookmarkCursor = mBookmarkDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TITLE, StateTable.COLUMN_TRACKS, StateTable.COLUMN_AUTOSAVE},
                "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

        if (bookmarkCursor != null) {

            if (bookmarkCursor.moveToFirst()) {
                do {
                    long timeStamp = bookmarkCursor.getLong(bookmarkCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));
                    String title = bookmarkCursor.getString(bookmarkCursor.getColumnIndex(StateTable.COLUMN_TITLE));
                    int numberOfTracks = bookmarkCursor.getInt(bookmarkCursor.getColumnIndex(StateTable.COLUMN_TRACKS));
                    int autosave = bookmarkCursor.getInt(bookmarkCursor.getColumnIndex(StateTable.COLUMN_AUTOSAVE));

                    bookmarks.add(new BookmarkModel(timeStamp, title, numberOfTracks, autosave));

                } while (bookmarkCursor.moveToNext());
            }

            bookmarkCursor.close();
        }

        return bookmarks;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
