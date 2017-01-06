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

package org.gateshipone.odyssey.models;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileModel implements GenericModel {

    /**
     * The file object for this instance
     */
    private final File mFile;

    /**
     * Helper class to compare to FileModel instances
     */
    private final FileModelComparator mFileModelComparator;

    /**
     * Helper class to filter specific file extensions
     */
    private final FileExtensionFilter mFileExtensionFilter;

    /**
     * Static list of valid file extensions
     */
    private static final List<String> fileExtensions = new ArrayList<>(Arrays.asList("3gp", "aac",
            "flac", "imy", "m4a", "mid", "mkv", "mp3", "mp4", "mxmf", "ogg", "oga", "opus", "ota", "rtttl",
            "rtx", "ts", "wav", "wma", "xmf"));

    /**
     * Construct a FileModel instance for the given file object.
     */
    public FileModel(File file) {
        mFile = file;

        mFileModelComparator = new FileModelComparator();
        mFileExtensionFilter = new FileExtensionFilter(fileExtensions);
    }

    /**
     * Construct a FileModel instance for the given file path.
     */
    public FileModel(String filePath) {
        mFile = new File(filePath);

        mFileModelComparator = new FileModelComparator();
        mFileExtensionFilter = new FileExtensionFilter(fileExtensions);
    }

    /**
     * Return the lastModified value of the file object
     */
    public long getLastModified() {
        return mFile.lastModified();
    }

    /**
     * Return the name of the file object
     */
    public String getName() {
        return mFile.getName();
    }

    /**
     * Return the path of the file object
     */
    public String getPath() {
        return mFile.getPath();
    }

    /**
     * Return the path of the file object as an url string
     */
    public String getURLString() {
        return mFile.getPath();
    }

    /**
     * Return if the file object is a directory
     */
    public boolean isDirectory() {
        return mFile.isDirectory();
    }

    /**
     * Return if the file object is a file
     */
    public boolean isFile() {
        return mFile.isFile();
    }

    public String getParent() {
        return mFile.getParent();
    }

    /**
     * Return a list of the files in the directory represented by the file object.
     * This list will contain FileModel objects sorted by the filename and the filetype.
     */
    public List<FileModel> listFilesSorted() {
        List<FileModel> files = new ArrayList<>();

        // get all files in the current folder
        File[] filesArray = mFile.listFiles(mFileExtensionFilter);

        if ( null == filesArray ) {
            return files;
        }
        // create FileModel instances
        for (File file : filesArray) {
            files.add(new FileModel(file));
        }

        // sort the list
        Collections.sort(files, mFileModelComparator);

        return files;
    }

    /**
     * Return the number of subfolders
     */
    public int getNumberOfSubFolders() {
        int numberOfSubFolders = 0;

        // get all files in the current folder
        File[] filesArray = mFile.listFiles(mFileExtensionFilter);

        if ( null == filesArray ) {
            return numberOfSubFolders;
        }

        for (File file : filesArray) {
            if (file.isDirectory()) {
                numberOfSubFolders++;
            }
        }

        return numberOfSubFolders;
    }

    /**
     * Return the section title of this class.
     * This will be the name of the file object.
     */
    @Override
    public String getSectionTitle() {
        return mFile.getName();
    }

    /**
     * Override equals method.
     * This method will only compare the two file objects.
     */
    @Override
    public boolean equals(Object model) {
        if (!(model instanceof FileModel)) {
            return false;
        }
        // just compare the two file objects
        return mFile.equals(((FileModel) model).mFile);
    }

    /**
     * Comparator class for the FileModel
     */
    private class FileModelComparator implements Comparator<FileModel> {

        @Override
        public int compare(FileModel f1, FileModel f2) {

            if (f1.equals(f2)) {
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
            return f1.getSectionTitle().compareToIgnoreCase(f2.getSectionTitle());
        }
    }

    /**
     * Filename filter class for the FileModel.
     */
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
