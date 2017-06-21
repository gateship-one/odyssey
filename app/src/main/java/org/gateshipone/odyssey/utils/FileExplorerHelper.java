/*
 * Copyright (C) 2017 Team Gateship-One
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
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;

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
     * Create a dummy {@link TrackModel} for the given {@link FileModel}.
     *
     * @param file The given {@link FileModel}.
     * @return A dummy {@link TrackModel} that only contains the file name and the uri.
     */
    public TrackModel getTrackModelForFile(FileModel file) {
        return new TrackModel(file.getName(), null, null, null, 0, -1, file.getURLString(), -1);
    }

    /**
     * Create a {@link TrackModel} for the given url.
     * <p>
     * This method will try to retrieve the track in the mediadb or try to extract the meta data using the {@link MediaMetadataRetriever}.
     * If both methods fail a dummy {@link TrackModel} will be created.
     *
     * @param context    The {@link Context} used to open the file and access the mediadb.
     * @param trackTitle The title for the {@link TrackModel} if a dummy track is created.
     * @param trackUrl   The given url for track as a String.
     * @return A valid {@link TrackModel}.
     */
    public TrackModel readTrackMetaData(final Context context, final String trackTitle, final String trackUrl) {
        // parse the given url
        Uri uri = FormatHelper.encodeURI(trackUrl);

        String whereVal[] = {uri.getPath()};

        String where = MediaStore.Audio.Media.DATA + "=?";

        if (uri.getScheme().equals("content")) {
            // special handling for content urls
            final String parts[] = uri.getLastPathSegment().split(":");

            if (parts.length > 1) {
                if (parts[0].equals("audio")) {
                    whereVal = new String[]{parts[1]};
                    where = MediaStore.Audio.Media._ID + "=?";
                } else {
                    whereVal = new String[]{"%" + parts[1]};
                    where = MediaStore.Audio.Media.DATA + " LIKE ?";
                }
            }
        }

        // lookup the current file in the media db
        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                cursor.close();

                return new TrackModel(title, artist, album, albumKey, duration, no, url, id);
            }

            cursor.close();
        }

        try {
            // try to read the file metadata

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            retriever.setDataSource(context, uri);

            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            long duration = 0;

            if (durationString != null) {
                try {
                    duration = Long.valueOf(durationString);
                } catch (NumberFormatException e) {
                    duration = 0;
                }
            }

            String noString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);

            int no = -1;

            if (noString != null) {
                try {
                    if (noString.contains("/")) {
                        // if string has the format (trackNumber / numberOfTracks)
                        String[] components = noString.split("/");
                        if (components.length > 0) {
                            no = Integer.valueOf(components[0]);
                        }
                    } else {
                        no = Integer.valueOf(noString);
                    }
                } catch (NumberFormatException e) {
                    no = -1;
                }
            }

            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            String albumKey = "" + ((artist == null ? "" : artist) + (album == null ? "" : album)).hashCode();

            return new TrackModel(title, artist, album, albumKey, duration, no, trackUrl, -1);
        } catch (Exception e) {
            // something went wrong so just create a dummy track with the given title
            String albumKey = "" + trackTitle.hashCode();
            return new TrackModel(trackTitle, null, null, albumKey, 0, -1, trackUrl, -1);
        }
    }

    /**
     * Updates the meta data of the tracks in the given list.
     *
     * @param context The {@link Context} used to open the file and access the mediadb.
     * @param tracks The track list. The elements of the list will be replaced.
     */
    public void updateTrackModels(final Context context, final List<TrackModel> tracks) {
        for (int i = 0; i < tracks.size(); i++) {
            tracks.set(i, readTrackMetaData(context, tracks.get(i).getTrackName(), tracks.get(i).getTrackURL()));
        }
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
                tracks.add(getTrackModelForFile(file));
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
        List<FileModel> filesFS = new ArrayList<>();
        List<FileModel> filesDB = new ArrayList<>();

        String whereVal[] = {basePath.getPath() + "%"};

        String where = MediaStore.Audio.Media.DATA + " LIKE ?";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    filesDB.add(new FileModel(url));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

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
     */
    public List<TrackModel> getTrackModelsForFolderAndSubFolders(Context context, FileModel folder) {
        List<TrackModel> tracks = new ArrayList<>();
        // check current folder and subfolders for music files
        getTrackModelsForFolderAndSubFolders(context, folder, tracks);

        return tracks;
    }

    /**
     * add TrackModel objects for the current folder and all subfolders to the tracklist
     */
    private void getTrackModelsForFolderAndSubFolders(Context context, FileModel folder, List<TrackModel> tracks) {
        if (folder.isFile()) {
            // file is not a directory so create a trackmodel for the file
            tracks.add(getTrackModelForFile(folder));
        } else {
            List<FileModel> files = PermissionHelper.getFilesForDirectory(context, folder);

            for (FileModel file : files) {
                // call method for all files found in this folder
                getTrackModelsForFolderAndSubFolders(context, file, tracks);
            }
        }
    }
}
