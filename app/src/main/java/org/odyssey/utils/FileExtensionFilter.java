package org.odyssey.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

/**
 * filename filter class for the filebrowser
 */
public class FileExtensionFilter implements FilenameFilter {

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
