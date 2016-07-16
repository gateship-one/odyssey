package org.odyssey.playbackservice.statemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.odyssey.models.TrackModel;

import java.util.ArrayList;
import java.util.Collections;

public class StateManager {
    private CurrentPlaylistDBHelper mCurrentPlaylistDBHelper;
    private SQLiteDatabase mPlaylistDB;

    private String[] projectionTrackModels = { SavedTracksTable.COLUMN_TRACKNUMBER, SavedTracksTable.COLUMN_TRACKTITLE, SavedTracksTable.COLUMN_TRACKALBUM, SavedTracksTable.COLUMN_TRACKALBUMKEY, SavedTracksTable.COLUMN_TRACKDURATION,
            SavedTracksTable.COLUMN_TRACKARTIST, SavedTracksTable.COLUMN_TRACKURL };

    public StateManager(Context context) {
        mCurrentPlaylistDBHelper = new CurrentPlaylistDBHelper(context);
        mPlaylistDB = mCurrentPlaylistDBHelper.getWritableDatabase();
    }

    public void savePlaylist(ArrayList<TrackModel> playList) {
        // clear the database
        mPlaylistDB.delete(SavedTracksTable.TABLE_NAME, null, null);

        ContentValues values = new ContentValues();

        mPlaylistDB.beginTransaction();

        for (TrackModel item : playList) {

            values.clear();

            // set TrackModel parameters
            values.put(SavedTracksTable.COLUMN_TRACKTITLE, item.getTrackName());
            values.put(SavedTracksTable.COLUMN_TRACKDURATION, item.getTrackDuration());
            values.put(SavedTracksTable.COLUMN_TRACKNUMBER, item.getTrackNumber());
            values.put(SavedTracksTable.COLUMN_TRACKARTIST, item.getTrackArtistName());
            values.put(SavedTracksTable.COLUMN_TRACKALBUM, item.getTrackAlbumName());
            values.put(SavedTracksTable.COLUMN_TRACKURL, item.getTrackURL());
            values.put(SavedTracksTable.COLUMN_TRACKALBUMKEY, item.getTrackAlbumKey());

            mPlaylistDB.insert(SavedTracksTable.TABLE_NAME, null, values);
        }

        mPlaylistDB.setTransactionSuccessful();
        mPlaylistDB.endTransaction();

    }

    public ArrayList<TrackModel> readPlaylist() {

        // get all TrackModels from database and return them

        ArrayList<TrackModel> playList = new ArrayList<>();

        Cursor cursor = mPlaylistDB.query(SavedTracksTable.TABLE_NAME, projectionTrackModels, "", null, "", "", SavedTracksTable.COLUMN_ID);

        if (cursor.moveToFirst()) {
            do {
                String trackName = cursor.getString(cursor.getColumnIndex(SavedTracksTable.COLUMN_TRACKTITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(SavedTracksTable.COLUMN_TRACKDURATION));
                int number = cursor.getInt(cursor.getColumnIndex(SavedTracksTable.COLUMN_TRACKNUMBER));
                String artistName = cursor.getString(cursor.getColumnIndex(SavedTracksTable.COLUMN_TRACKARTIST));
                String albumName = cursor.getString(cursor.getColumnIndex(SavedTracksTable.COLUMN_TRACKALBUM));
                String url = cursor.getString(cursor.getColumnIndex(SavedTracksTable.COLUMN_TRACKURL));
                String albumKey = cursor.getString(cursor.getColumnIndex(SavedTracksTable.COLUMN_TRACKALBUMKEY));

                TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url);

                playList.add(item);

            } while (cursor.moveToNext());
        }

        cursor.close();

        return playList;
    }

    public void clearPlaylist() {

        // clear the database
        mPlaylistDB.delete(SavedTracksTable.TABLE_NAME, null, null);
    }

    public int getSize() {

        // get number of rows in the database

        String[] projection = { SavedTracksTable.COLUMN_ID };

        Cursor cursor = mPlaylistDB.query(SavedTracksTable.TABLE_NAME, projection, "", null, "", "", SavedTracksTable.COLUMN_ID);

        int size = cursor.getCount();

        cursor.close();

        return size;
    }

    public void saveCurrentPlayState(long position, long trackNR, int random, int repeat) {
        // Delete old settings rows
        String whereStmt = StateTable.COLUMN_SETTINGSNAME + "=? OR " + StateTable.COLUMN_SETTINGSNAME + "=? OR " + StateTable.COLUMN_SETTINGSNAME + "=? OR " + StateTable.COLUMN_SETTINGSNAME + "=?";
        String[] whereArgs = { StateTable.TRACKNUMBER_ROW, StateTable.TRACKPOSITION_ROW, StateTable.RANDOM_STATE_ROW, StateTable.REPEAT_STATE_ROW };
        mPlaylistDB.delete(StateTable.TABLE_NAME, whereStmt, whereArgs);

        // Insert new values into table
        String positionStmt = "INSERT INTO " + StateTable.TABLE_NAME + " values ( \"" + StateTable.TRACKPOSITION_ROW + "\"," + "\"" + position + "\");";
        mPlaylistDB.execSQL(positionStmt);

        String nrStmt = "INSERT INTO " + StateTable.TABLE_NAME + " values ( \"" + StateTable.TRACKNUMBER_ROW + "\"," + "\"" + trackNR + "\");";
        mPlaylistDB.execSQL(nrStmt);

        String randomStmt = "INSERT INTO " + StateTable.TABLE_NAME + " values ( \"" + StateTable.RANDOM_STATE_ROW + "\"," + "\"" + random + "\");";
        mPlaylistDB.execSQL(randomStmt);

        String repeatStmt = "INSERT INTO " + StateTable.TABLE_NAME + " values ( \"" + StateTable.REPEAT_STATE_ROW + "\"," + "\"" + repeat + "\");";
        mPlaylistDB.execSQL(repeatStmt);
    }

    public long getLastTrackPosition() {
        String[] columns = { StateTable.COLUMN_SETTINGSVALUE };
        String selection = StateTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { StateTable.TRACKPOSITION_ROW };
        Cursor resultCursor = mPlaylistDB.query(StateTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        long value = 0;
        if (resultCursor.moveToFirst()) {
            value = resultCursor.getLong(resultCursor.getColumnIndex(StateTable.COLUMN_SETTINGSVALUE));
        }
        resultCursor.close();
        return value;
    }

    public long getLastTrackNumber() {
        String[] columns = { StateTable.COLUMN_SETTINGSVALUE };
        String selection = StateTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { StateTable.TRACKNUMBER_ROW };
        Cursor resultCursor = mPlaylistDB.query(StateTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        long value = 0;
        if (resultCursor.moveToFirst()) {
            value = resultCursor.getLong(resultCursor.getColumnIndex(StateTable.COLUMN_SETTINGSVALUE));
        }
        resultCursor.close();
        return value;
    }

    public int getLastRandomState() {
        String[] columns = { StateTable.COLUMN_SETTINGSVALUE };
        String selection = StateTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { StateTable.RANDOM_STATE_ROW };
        Cursor resultCursor = mPlaylistDB.query(StateTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        int value = 0;
        if (resultCursor.moveToFirst()) {
            value = resultCursor.getInt(resultCursor.getColumnIndex(StateTable.COLUMN_SETTINGSVALUE));
        }
        resultCursor.close();
        return value;
    }

    public int getLastRepeatState() {
        String[] columns = { StateTable.COLUMN_SETTINGSVALUE };
        String selection = StateTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { StateTable.REPEAT_STATE_ROW };
        Cursor resultCursor = mPlaylistDB.query(StateTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        int value = 0;
        if (resultCursor.moveToFirst()) {
            value = resultCursor.getInt(resultCursor.getColumnIndex(StateTable.COLUMN_SETTINGSVALUE));
        }
        resultCursor.close();
        return value;
    }
}
