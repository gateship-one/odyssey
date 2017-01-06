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

package org.gateshipone.odyssey.utils;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileExplorerHelper {

    private static FileExplorerHelper mInstance = null;
    private HashMap<String, TrackModel> mTrackHash;

    private final static String TAG = "FileExplorerHelper";

    private FileExplorerHelper() {
        mTrackHash = new HashMap<>();
    }

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
        TrackModel track = null;

        String urlString = file.getURLString();

        if (mTrackHash.isEmpty()) {
            // lookup the current file in the media db
            String whereVal[] = {urlString};

            String where = MediaStore.Audio.Media.DATA + "=?";

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

                    track = new TrackModel(title, artist, album, albumKey, duration, no, url, id);
                }

                cursor.close();
            }
        } else {
            // use pre built hash to lookup the file
            track = mTrackHash.get(urlString);
        }

        if (track == null) {
            // no entry in the media db was found so create a custom track
            try {
                // try to read the file metadata

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();

                retriever.setDataSource(context, Uri.parse(FormatHelper.encodeFileURI(urlString)));

                String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

                if (title == null) {
                    title = file.getName();
                }

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

                track = new TrackModel(title, artist, album, albumKey, duration, no, urlString, -1);
            } catch (Exception e) {
                String albumKey = "" + file.getName().hashCode();
                track = new TrackModel(file.getName(), "", "", albumKey, 0, -1, urlString, -1);
            }
        }

        return track;
    }

    /**
     * return a list of TrackModels created for the given folder
     * this excludes all subfolders
     */
    public List<TrackModel> getTrackModelsForFolder(Context context, FileModel folder) {
        List<TrackModel> tracks = new ArrayList<>();

        // get all tracks from the mediadb related to the current folder and store the tracks in a hashmap
        mTrackHash.clear();

        String urlString = folder.getURLString();
        String whereVal[] = {urlString + "%"};

        String where = MediaStore.Audio.Media.DATA + " LIKE ?";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                    TrackModel track = new TrackModel(title, artist, album, albumKey, duration, no, url, id);

                    mTrackHash.put(url, track);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        List<FileModel> files = PermissionHelper.getFilesForDirectory(context, folder);

        for (FileModel file : files) {
            if (file.isFile()) {
                // file is not a directory so create a trackmodel for the file
                tracks.add(getTrackModelForFile(context, file));
            }
        }

        // clear the hash
        mTrackHash.clear();

        return tracks;
    }

    /**
     * return a list of TrackModels created for the given folder
     * this includes all subfolders
     */
    public List<TrackModel> getTrackModelsForFolderAndSubFolders(Context context, FileModel folder) {
        List<TrackModel> tracks = new ArrayList<>();

        // get all tracks from the mediadb related to the current folder and store the tracks in a hashmap
        mTrackHash.clear();

        String urlString = folder.getURLString();
        String whereVal[] = {urlString + "%"};

        String where = MediaStore.Audio.Media.DATA + " LIKE ?";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                    TrackModel track = new TrackModel(title, artist, album, albumKey, duration, no, url, id);

                    mTrackHash.put(url, track);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        // check current folder and subfolders for music files
        getTrackModelsForFolderAndSubFolders(context, folder, tracks);

        // clear the hash
        mTrackHash.clear();

        return tracks;
    }

    /**
     * add TrackModel objects for the current folder and all subfolders to the tracklist
     */
    private void getTrackModelsForFolderAndSubFolders(Context context, FileModel folder, List<TrackModel> tracks) {
        if (folder.isFile()) {
            // file is not a directory so create a trackmodel for the file
            tracks.add(getTrackModelForFile(context, folder));
        } else {
            List<FileModel> files = PermissionHelper.getFilesForDirectory(context, folder);

            for (FileModel file : files) {
                // call method for all files found in this folder
                getTrackModelsForFolderAndSubFolders(context, file, tracks);
            }
        }
    }
}
