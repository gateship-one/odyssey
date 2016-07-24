package org.odyssey.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import org.odyssey.models.BookmarkModel;
import org.odyssey.playbackservice.statemanager.OdysseyDatabaseManager;
import java.util.List;

public class BookmarkLoader extends AsyncTaskLoader<List<BookmarkModel>> {
    private final Context mContext;

    public BookmarkLoader(Context context) {
        super(context);

        mContext = context;
    }

    /**
     * Load all bookmarks from the database.
     */
    @Override
    public List<BookmarkModel> loadInBackground() {
        return new OdysseyDatabaseManager(mContext).getBookmarks();
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
