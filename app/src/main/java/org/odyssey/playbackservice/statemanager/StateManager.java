package org.odyssey.playbackservice.statemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.OdysseyServiceState;

import java.util.ArrayList;
import java.util.List;

public class StateManager {
    public static final String TAG = "OdysseyStateManager";

    private CurrentPlaylistDBHelper mCurrentPlaylistDBHelper;
    private SQLiteDatabase mPlaylistDB;

    private String[] projectionTrackModels = {SavedTracksTable.COLUMN_TRACKNUMBER, SavedTracksTable.COLUMN_TRACKTITLE, SavedTracksTable.COLUMN_TRACKALBUM, SavedTracksTable.COLUMN_TRACKALBUMKEY,
            SavedTracksTable.COLUMN_TRACKDURATION, SavedTracksTable.COLUMN_TRACKARTIST, SavedTracksTable.COLUMN_TRACKURL};

    private String[] projectionState = {StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TRACKNUMBER, StateTable.COLUMN_TRACKPOSITION, StateTable.COLUMN_RANDOM_STATE, StateTable.COLUMN_REPEAT_STATE};

    public StateManager(Context context) {
        mCurrentPlaylistDBHelper = new CurrentPlaylistDBHelper(context);
        mPlaylistDB = mCurrentPlaylistDBHelper.getWritableDatabase();
    }

    public void saveState(List<TrackModel> playList, OdysseyServiceState state, String title, boolean autosave) {
        Log.v(TAG, "save state");

        if (autosave) {
            // delete previous auto saved states if this save is an auto generated save
            clearAutoSaveState();
        }

        long timeStamp = System.currentTimeMillis();

        savePlaylist(playList, timeStamp);

        saveCurrentPlayState(state, timeStamp, autosave, title, playList.size());
    }

    private void savePlaylist(List<TrackModel> playList, long timeStamp) {
        // save the given playlist in the database with the given timestamp as an additional identifier

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
            values.put(SavedTracksTable.COLUMN_BOOKMARK_TIMESTAMP, timeStamp);

            mPlaylistDB.insert(SavedTracksTable.TABLE_NAME, null, values);
        }

        mPlaylistDB.setTransactionSuccessful();
        mPlaylistDB.endTransaction();
    }

    public List<TrackModel> readPlaylist(long timeStamp) {
        // get all TrackModels from database for the given timestamp and return them

        List<TrackModel> playList = new ArrayList<>();

        Cursor cursor = mPlaylistDB.query(SavedTracksTable.TABLE_NAME, projectionTrackModels, SavedTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)},
                "", "", SavedTracksTable.COLUMN_ID);

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

    public List<TrackModel> readPlaylist() {
        // get all TrackModels from database for most recent timestamp and return them

        List<TrackModel> playList = new ArrayList<>();

        // query the most recent timestamp
        Cursor stateCursor = mPlaylistDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP}, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC", "1");

        if (stateCursor.moveToFirst()) {
            long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

            // get the playlist tracks for the queried timestamp
            Cursor cursor = mPlaylistDB.query(SavedTracksTable.TABLE_NAME, projectionTrackModels, SavedTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)},
                    "", "", SavedTracksTable.COLUMN_ID);

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
        }

        stateCursor.close();

        return playList;
    }

    private void saveCurrentPlayState(OdysseyServiceState state, long timeStamp, boolean autosave, String title, int numberOfTracks) {

        ContentValues values = new ContentValues();

        mPlaylistDB.beginTransaction();

        // set state parameters
        values.put(StateTable.COLUMN_BOOKMARK_TIMESTAMP, timeStamp);
        values.put(StateTable.COLUMN_TRACKNUMBER, state.mTrackNumber);
        values.put(StateTable.COLUMN_TRACKPOSITION, state.mTrackPosition);
        values.put(StateTable.COLUMN_RANDOM_STATE, state.mRandomState);
        values.put(StateTable.COLUMN_REPEAT_STATE, state.mRepeatState);
        values.put(StateTable.COLUMN_AUTOSAVE, autosave);
        values.put(StateTable.COLUMN_TITLE, title);
        values.put(StateTable.COLUMN_TRACKS, numberOfTracks);

        mPlaylistDB.insert(StateTable.TABLE_NAME, null, values);

        mPlaylistDB.setTransactionSuccessful();
        mPlaylistDB.endTransaction();
    }

    public OdysseyServiceState getState(long timeStamp) {

        // get state for given timestamp in db
        OdysseyServiceState state = new OdysseyServiceState();

        Cursor cursor = mPlaylistDB.query(StateTable.TABLE_NAME, projectionState, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)}, "", "", "");

        if (cursor.moveToFirst()) {

            state.mTrackNumber = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKNUMBER));
            state.mTrackPosition = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKPOSITION));
            state.mRandomState = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_RANDOM_STATE));
            state.mRepeatState = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_REPEAT_STATE));
        }

        cursor.close();

        return state;
    }

    public OdysseyServiceState getState() {
        // get state for most recent timestamp in db
        OdysseyServiceState state = new OdysseyServiceState();

        Cursor cursor = mPlaylistDB.query(StateTable.TABLE_NAME, projectionState, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC", "1");

        if (cursor.moveToFirst()) {

            state.mTrackNumber = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKNUMBER));
            state.mTrackPosition = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKPOSITION));
            state.mRandomState = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_RANDOM_STATE));
            state.mRepeatState = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_REPEAT_STATE));
        }

        cursor.close();

        return state;
    }

    public void removeState(long timestamp) {
        // remove the state and playlist for the given timestamp

        // delete playlist
        mPlaylistDB.delete(SavedTracksTable.TABLE_NAME, SavedTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});

        // delete state
        mPlaylistDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});
    }

    private void clearAutoSaveState() {
        // remove all auto save states and playlist from db
        Cursor stateCursor = mPlaylistDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_AUTOSAVE}, StateTable.COLUMN_AUTOSAVE + "=?", new String[]{"1"},
                "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

        if (stateCursor.moveToFirst()) {
            do {
                long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

                // delete playlist
                mPlaylistDB.delete(SavedTracksTable.TABLE_NAME, SavedTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});

                // delete state
                mPlaylistDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});
            } while (stateCursor.moveToNext());
        }

        stateCursor.close();
    }
}
