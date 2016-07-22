package org.odyssey.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileLoader extends AsyncTaskLoader<List<File>> {

    private final File mCurrentDirectory;
    private final FileExtensionFilter mFileExtensionFilter;
    private final FileComparator mFileComparator;

    // todo add extensions
    private static List<String> mFileExtensions = new ArrayList<>(Arrays.asList("mp3", "flac", "wav", "ogg"));

    public FileLoader(Context context, File directory) {
        super(context);

        mCurrentDirectory = directory;

        mFileExtensionFilter = new FileExtensionFilter(mFileExtensions);
        mFileComparator = new FileComparator();
    }

    @Override
    public List<File> loadInBackground() {

        List<File> files = new ArrayList<>();

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

    private class FileComparator implements Comparator<File> {

        @Override
        public int compare(File f1, File f2) {

            if (f1 == f2) {
                return 0;
            }

            if (f1.isDirectory() && f2.isFile()) {
                // show directories above files
                return -1;
            }

            if (f1.isFile() && f2.isDirectory()) {
                // show files below directories
                return 1;
            }

            // sort alphabetically, ignoring case
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    private class FileExtensionFilter implements FilenameFilter {

        private List<String> mExtensions;

        public FileExtensionFilter(List<String> extensions) {
            mExtensions = extensions;
        }

        @Override
        public boolean accept(File dir, String filename) {

            // check if directory
            if (new File(dir, filename).isDirectory()) {
                // show all directories
                return true;
            }

            // check if file matches the valid extensions

            if (!mExtensions.isEmpty()) {

                // get file extension
                String ext = getFileExtension(filename);

                if (mExtensions.contains(ext)) {
                    // filename has valid extension
                    return true;
                }
            }

            // filename has no valid extension
            return false;
        }

        private String getFileExtension(String filename) {
            // get the extension of the given filename

            String ext = null;
            int i = filename.lastIndexOf('.');
            if (i != -1 && i < filename.length()) {
                ext = filename.substring(i + 1).toLowerCase();
            }
            return ext;
        }
    }
}
