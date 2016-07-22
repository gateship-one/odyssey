package org.odyssey.utils;

import java.io.File;
import java.util.Comparator;

/**
 * comparator class for the filebrowser
 */
public class FileComparator implements Comparator<File> {

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