package org.odyssey.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import org.odyssey.models.FileModel;

import java.util.List;

public class FileLoader extends AsyncTaskLoader<List<FileModel>> {

    private final FileModel mCurrentDirectory;

    public FileLoader(Context context, FileModel directory) {
        super(context);

        mCurrentDirectory = directory;
    }

    /**
     * Load all FileModel objects for the given directory FileModel.
     */
    @Override
    public List<FileModel> loadInBackground() {

        return mCurrentDirectory.listFilesSorted();
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
