/*
 * Copyright (C) 2020 Team Gateship-One
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

package org.gateshipone.odyssey.utils;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import org.gateshipone.odyssey.database.MusicDatabaseFactory;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class FileExplorerHelper {

    private static FileExplorerHelper mInstance = null;

    public static synchronized FileExplorerHelper getInstance() {
        if (mInstance == null) {
            mInstance = new FileExplorerHelper();
        }

        return mInstance;
    }

    /**
     * return the list of available storage volumes
     */
    public List<String> getStorageVolumes(Context context) {
        // create storage volume list
        ArrayList<String> storagePathList = new ArrayList<>();

        File[] storageList = ContextCompat.getExternalFilesDirs(context, null);
        for (File storageFile : storageList) {
            if (null != storageFile) {
                storagePathList.add(storageFile.getAbsolutePath().replaceAll("/Android/data/" + context.getPackageName() + "/files", ""));
            }
        }
        storagePathList.add("/");

        return storagePathList;
    }

    /**
     * create a TrackModel for the given File
     * if no entry in the mediadb is found a dummy TrackModel will be created
     */
    public TrackModel getTrackModelForFile(Context context, FileModel file) {
        // parse the given url
        final Uri uri = FormatHelper.encodeURI(file.getURLString());

        // lookup the current file in the media db
        final TrackModel track = MusicDatabaseFactory.getDatabase(context).getTrackForUri(uri, context);

        if (null == track) {
            return MusicDatabaseFactory.getDatabase(context).getDummyTrackModel(file);
        }

        return track;
    }

    /**
     * return a list of TrackModels created for the given folder
     * this excludes all subfolders
     */
    public List<TrackModel> getTrackModelsForFolder(Context context, FileModel folder) {
        List<TrackModel> tracks = new ArrayList<>();

        List<FileModel> files = PermissionHelper.getFilesForDirectory(context, folder);

        for (FileModel file : files) {
            if (file.isFile()) {
                // file is not a directory so create a trackmodel for the file
                if (file.isPlaylist()) {
                    // Parse the playlist file with a parser
                    PlaylistParser parser = PlaylistParserFactory.getParser(file);
                    if (parser != null) {
                        tracks.addAll(parser.parseList(context));
                    }
                } else {
                    tracks.add(MusicDatabaseFactory.getDatabase(context).getDummyTrackModel(file));
                }
            }
        }

        return tracks;
    }

    /**
     * Generates a list of {@link FileModel} objects that are either in the Android DB and not on the FS
     * or that are on the FS but not in the Android DB.
     *
     * @param context  Context used for DB query
     * @param basePath Path of files to check
     * @return List of files that need to be scanned
     */
    public List<FileModel> getMissingDBFiles(Context context, FileModel basePath) {
        List<FileModel> filesDB = MusicDatabaseFactory.getDatabase(context).getMediaFilesForPath(basePath.getPath(), context);
        List<FileModel> filesFS = new ArrayList<>();

        getFilesRecursive(context, basePath, filesFS);

        return generateFileListDiff(filesDB, filesFS);
    }

    /**
     * Helper method to create a list of {@link FileModel} objects that are part of one list but not
     * of the other.
     *
     * @param list1 First list of {@link FileModel} objects.
     * @param list2 Second list of {@link FileModel} objects.
     * @return List of {@link FileModel} that are only part of one of the two given lists.
     */
    private List<FileModel> generateFileListDiff(List<FileModel> list1, List<FileModel> list2) {
        List<FileModel> filesDiff = new ArrayList<>();

        // Sort lists so that an easy compare is possible because of given order.
        Collections.sort(list1);
        Collections.sort(list2);

        // Create the difference between the to lists
        ListIterator<FileModel> dbIterartor = list1.listIterator();
        ListIterator<FileModel> fsIterartor = list2.listIterator();

        // Get the first list elements (if any available)
        FileModel list1Model = null;
        if (dbIterartor.hasNext()) {
            list1Model = dbIterartor.next();
        }
        FileModel list2Model = null;
        if (fsIterartor.hasNext()) {
            list2Model = fsIterartor.next();
        }

        while (dbIterartor.hasNext() || fsIterartor.hasNext()) {
            int compareVal = 0;

            // Check if files are available and compare them if both are available
            if (list1Model != null && list2Model != null) {
                compareVal = list1Model.compareTo(list2Model);
            } else if (list1Model != null) {
                // No model from the list2 available, make sure remaining list1 models are added
                compareVal = -1;
            } else if (list2Model != null) {
                // No model from the list1 available, make sure remaining list2 models are added
                compareVal = 1;
            }
            if (compareVal == 0) {
                // Both are equal, move to next elements on both lists
                if (dbIterartor.hasNext()) {
                    list1Model = dbIterartor.next();
                } else {
                    list1Model = null;
                }
                if (fsIterartor.hasNext()) {
                    list2Model = fsIterartor.next();
                } else {
                    list2Model = null;
                }
            } else if (compareVal < 0) {
                // list1Model is less, move it forward and add current file
                filesDiff.add(list1Model);
                if (dbIterartor.hasNext()) {
                    list1Model = dbIterartor.next();
                } else {
                    list1Model = null;
                }
            } else {
                // list2Model is less, move it forward and add current file
                filesDiff.add(list2Model);
                if (fsIterartor.hasNext()) {
                    list2Model = fsIterartor.next();
                } else {
                    list2Model = null;
                }
            }
        }
        return filesDiff;
    }

    /**
     * Helper method to create a list of of {@link FileModel} objects that are files for the given folder.
     * This method will be called recursively for all subfolders.
     *
     * @param context The current android context.
     * @param folder  The current folder.
     * @param files   List of {@link FileModel} objects that are files and in the folder.
     */
    private void getFilesRecursive(Context context, FileModel folder, List<FileModel> files) {
        if (folder.isFile()) {
            // file is not a directory so add the filemodel to the list
            files.add(folder);
        } else {
            List<FileModel> filesTmp = PermissionHelper.getFilesForDirectory(context, folder);

            for (FileModel file : filesTmp) {
                // call method for all files found in this folder
                getFilesRecursive(context, file, files);
            }
        }
    }

    /**
     * return a list of TrackModels created for the given folder
     * this includes all subfolders
     *
     * @param filterString A filter that is used to exclude folders/files that didn't contain this String.
     */
    public List<TrackModel> getTrackModelsForFolderAndSubFolders(Context context, FileModel folder, String filterString) {
        List<TrackModel> tracks = new ArrayList<>();
        // check current folder and subfolders for music files
        getTrackModelsForFolderAndSubFolders(context, folder, tracks, filterString);

        return tracks;
    }

    /**
     * add TrackModel objects for the current folder and all subfolders to the tracklist
     *
     * @param filterString A filter that is used to exclude folders/files that didn't contain this String.
     */
    private void getTrackModelsForFolderAndSubFolders(Context context, FileModel folder, List<TrackModel> tracks, String filterString) {
        if (folder.isFile()) {
            if (filterString == null || filterString.isEmpty() || folder.getName().toLowerCase().contains(filterString)) {
                // file is not a directory so create a trackmodel for the file
                if (folder.isPlaylist()) {
                    // Parse the playlist file with a parser
                    PlaylistParser parser = PlaylistParserFactory.getParser(folder);
                    if (parser != null) {

                        tracks.addAll(parser.parseList(context));
                    }
                } else {
                    tracks.add(MusicDatabaseFactory.getDatabase(context).getDummyTrackModel(folder));
                }
            }
        } else {
            List<FileModel> files = PermissionHelper.getFilesForDirectory(context, folder);

            for (FileModel file : files) {
                if (filterString == null || filterString.isEmpty() || file.getName().toLowerCase().contains(filterString)) {
                    // call method for all files found in this folder
                    getTrackModelsForFolderAndSubFolders(context, file, tracks, null);
                }
            }
        }
    }
}
