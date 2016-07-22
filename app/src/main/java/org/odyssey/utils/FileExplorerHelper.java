package org.odyssey.utils;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.odyssey.models.TrackModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FileExplorerHelper {

    private static FileExplorerHelper mInstance = null;
    private Context mContext = null;
    private List<String> mStorageVolumesList;
    private HashMap<String, TrackModel> mTrackHash;

    private final FileExtensionFilter mFileExtensionFilter;
    private final FileComparator mFileComparator;

    // todo add extensions
    private static List<String> mFileExtensions = new ArrayList<>(Arrays.asList("mp3", "flac", "wav", "ogg"));

    private final static String TAG = "FileExplorerHelper";

    private FileExplorerHelper(Context context) {
        mContext = context;

        mFileExtensionFilter = new FileExtensionFilter(mFileExtensions);
        mFileComparator = new FileComparator();

        mTrackHash = new HashMap<>();

        // create storage volume list
        mStorageVolumesList = new ArrayList<>();

        File[] storageList = ContextCompat.getExternalFilesDirs(mContext, null);
        for (File storageFile : storageList) {
            mStorageVolumesList.add(storageFile.getAbsolutePath().replaceAll("/Android/data/" + mContext.getPackageName() + "/files", ""));
        }
    }

    public static synchronized FileExplorerHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FileExplorerHelper(context);
        }

        return mInstance;
    }

    /**
     * return the list of available storage volumes
     */
    public List<String> getStorageVolumes() {
        return mStorageVolumesList;
    }

    /**
     * return a list of valid fileextensions
     */
    public List<String> getValidFileExtensions() {
        return mFileExtensions;
    }

    /**
     * create a TrackModel for the given File
     * if no entry in the mediadb is found a dummy TrackModel will be created
     */
    public TrackModel getTrackModelForFile(File file) {
        TrackModel track = null;

        String urlString = file.toString();

        if (mTrackHash.isEmpty()) {
            // lookup the current file in the media db
            String whereVal[] = {urlString};

            String where = MediaStore.Audio.Media.DATA + "=?";

            Cursor cursor = PermissionHelper.query(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

                    track = new TrackModel(title, artist, album, albumKey, duration, no, url);
                }

                cursor.close();
            }
        } else {
            // use pre built hash to lookup the file
            track = mTrackHash.get(urlString);
        }

        if (track == null) {
            // no entry in the media db was found so create a dummy track
            track = new TrackModel(file.getName(), "Dummyartist", "Dummyalbum", "", 0, 1, urlString);
        }

        return track;
    }

    /**
     * return a list of TrackModels created for the given folder
     * this includes all subfolders
     */
    public List<TrackModel> getTrackModelsForFolder(File folder) {
        List<TrackModel> tracks = new ArrayList<>();

        // get all tracks from the mediadb related to the current folder and store the tracks in a hashmap
        Log.v(TAG, "create track hash");
        mTrackHash.clear();

        String urlString = folder.toString();
        String whereVal[] = {urlString+"%"};

        String where = MediaStore.Audio.Media.DATA + " LIKE ?";

        Cursor cursor = PermissionHelper.query(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);

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

                    TrackModel track = new TrackModel(title, artist, album, albumKey, duration, no, url);

                    mTrackHash.put(url, track);
                } while(cursor.moveToNext());
            }

            cursor.close();
        }

        Log.v(TAG, "create track hash done");

        Log.v(TAG, "create track models");
        // check current folder and subfolders for music files
        getTrackModelsForFolder(folder, tracks);
        Log.v(TAG, "create track models done");

        return tracks;
    }

    /**
     * add TrackModel objects for the current folder and all subfolders to the tracklist
     */
    public void getTrackModelsForFolder(File folder, List<TrackModel> tracks) {
        if (folder.isFile()) {
            // file is not a directory so create a trackmodel for the file
            tracks.add(getTrackModelForFile(folder));
        } else {
            List<File> files = new ArrayList<>();

            // get all valid files
            File[] filesArray = folder.listFiles(mFileExtensionFilter);
            Collections.addAll(files, filesArray);

            // sort the loaded files
            Collections.sort(files, mFileComparator);

            for (File file : files) {
                // call method for all files found in this folder
                getTrackModelsForFolder(file, tracks);
            }
        }
    }
}
