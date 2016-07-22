package org.odyssey.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import org.odyssey.utils.FileComparator;
import org.odyssey.utils.FileExtensionFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileLoader extends AsyncTaskLoader<List<File>> {

    private final File mCurrentDirectory;
    private final FileExtensionFilter mFileExtensionFilter;
    private final FileComparator mFileComparator;

    public FileLoader(Context context, File directory, List<String> validExtensions) {
        super(context);

        mCurrentDirectory = directory;

        mFileExtensionFilter = new FileExtensionFilter(validExtensions);
        mFileComparator = new FileComparator();
    }

    @Override
    public List<File> loadInBackground() {

        List<File> files = new ArrayList<>();

        // get all valid files
        File[] filesArray = mCurrentDirectory.listFiles(mFileExtensionFilter);
        Collections.addAll(files, filesArray);

        // sort the loaded files
        Collections.sort(files, mFileComparator);

        return files;
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
