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

package org.odyssey.playbackservice.statemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.odyssey.models.BookmarkModel;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.OdysseyServiceState;
import org.odyssey.playbackservice.PlaybackService;

import java.util.ArrayList;
import java.util.List;

public class OdysseyDatabaseManager extends SQLiteOpenHelper {
    public static final String TAG = "OdysseyStateManager";

    /**
     * The name of the database
     */
    private static final String DATABASE_NAME = "OdysseyStatesDB";
    /**
     * The version of the database
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Array of returned columns from the StateTracks table
     */
    private String[] projectionTrackModels = {StateTracksTable.COLUMN_TRACKNUMBER, StateTracksTable.COLUMN_TRACKTITLE, StateTracksTable.COLUMN_TRACKALBUM, StateTracksTable.COLUMN_TRACKALBUMKEY,
            StateTracksTable.COLUMN_TRACKDURATION, StateTracksTable.COLUMN_TRACKARTIST, StateTracksTable.COLUMN_TRACKURL, StateTracksTable.COLUMN_TRACKID};

    /**
     * Array of returned columns from the State table
     */
    private String[] projectionState = {StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TRACKNUMBER, StateTable.COLUMN_TRACKPOSITION, StateTable.COLUMN_RANDOM_STATE, StateTable.COLUMN_REPEAT_STATE};

    public OdysseyDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * This method creates the StateTracks and the State table
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        StateTracksTable.onCreate(db);
        StateTable.onCreate(db);
    }

    /**
     * Called when the database needs to be upgraded.
     * This method is currently not implemented.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Save a given state in the database, including the related playlist.
     * If an auto generated state is saved, all previous auto states will be deleted.
     *
     * @param playList The list of tracks for the current state
     * @param state    The current state
     * @param title    The title of this state
     * @param autosave True if it's an auto generated state
     */
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

    /**
     * Save the playlist in the database.
     *
     * @param playList  The list of tracks
     * @param timeStamp The timestamp as an additional identifier
     */
    private void savePlaylist(List<TrackModel> playList, long timeStamp) {

        SQLiteDatabase odysseyStateDB = getWritableDatabase();

        ContentValues values = new ContentValues();

        odysseyStateDB.beginTransaction();

        for (TrackModel item : playList) {

            values.clear();

            // set TrackModel parameters
            values.put(StateTracksTable.COLUMN_TRACKTITLE, item.getTrackName());
            values.put(StateTracksTable.COLUMN_TRACKDURATION, item.getTrackDuration());
            values.put(StateTracksTable.COLUMN_TRACKNUMBER, item.getTrackNumber());
            values.put(StateTracksTable.COLUMN_TRACKARTIST, item.getTrackArtistName());
            values.put(StateTracksTable.COLUMN_TRACKALBUM, item.getTrackAlbumName());
            values.put(StateTracksTable.COLUMN_TRACKURL, item.getTrackURL());
            values.put(StateTracksTable.COLUMN_TRACKALBUMKEY, item.getTrackAlbumKey());
            values.put(StateTracksTable.COLUMN_TRACKID, item.getTrackId());
            values.put(StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP, timeStamp);

            odysseyStateDB.insert(StateTracksTable.TABLE_NAME, null, values);
        }

        odysseyStateDB.setTransactionSuccessful();
        odysseyStateDB.endTransaction();

        // close the connection
        odysseyStateDB.close();
    }

    /**
     * Return the playlist for the given timestamp
     */
    public List<TrackModel> readPlaylist(long timeStamp) {

        SQLiteDatabase odysseyStateDB = getReadableDatabase();

        List<TrackModel> playList = new ArrayList<>();

        Cursor cursor = odysseyStateDB.query(StateTracksTable.TABLE_NAME, projectionTrackModels, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)},
                "", "", StateTracksTable.COLUMN_ID);

        if (cursor.moveToFirst()) {
            do {
                String trackName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKTITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKDURATION));
                int number = cursor.getInt(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKNUMBER));
                String artistName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKARTIST));
                String albumName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUM));
                String url = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKURL));
                String albumKey = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUMKEY));
                long id = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKID));

                TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id);

                playList.add(item);

            } while (cursor.moveToNext());
        }

        cursor.close();

        odysseyStateDB.close();

        return playList;
    }

    /**
     * Returns the playlist for the most recent timestamp
     */
    public List<TrackModel> readPlaylist() {

        SQLiteDatabase odysseyStateDB = getReadableDatabase();

        List<TrackModel> playList = new ArrayList<>();

        // query the most recent timestamp
        Cursor stateCursor = odysseyStateDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP}, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC", "1");

        if (stateCursor.moveToFirst()) {
            long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

            // get the playlist tracks for the queried timestamp
            Cursor cursor = odysseyStateDB.query(StateTracksTable.TABLE_NAME, projectionTrackModels, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)},
                    "", "", StateTracksTable.COLUMN_ID);

            if (cursor.moveToFirst()) {
                do {
                    String trackName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKTITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKDURATION));
                    int number = cursor.getInt(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKNUMBER));
                    String artistName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKARTIST));
                    String albumName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKURL));
                    String albumKey = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUMKEY));
                    long id = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKID));

                    TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id);

                    playList.add(item);

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        stateCursor.close();

        odysseyStateDB.close();

        return playList;
    }

    /**
     * Save the current state in the database.
     *
     * @param state          The state object
     * @param timeStamp      The given timestamp
     * @param autosave       True if it's an auto generated state
     * @param title          The title of the state
     * @param numberOfTracks The number of tracks related to this state
     */
    private void saveCurrentPlayState(OdysseyServiceState state, long timeStamp, boolean autosave, String title, int numberOfTracks) {

        SQLiteDatabase odysseyStateDB = getWritableDatabase();

        ContentValues values = new ContentValues();

        odysseyStateDB.beginTransaction();

        // set state parameters
        values.put(StateTable.COLUMN_BOOKMARK_TIMESTAMP, timeStamp);
        values.put(StateTable.COLUMN_TRACKNUMBER, state.mTrackNumber);
        values.put(StateTable.COLUMN_TRACKPOSITION, state.mTrackPosition);
        values.put(StateTable.COLUMN_RANDOM_STATE, state.mRandomState.ordinal());
        values.put(StateTable.COLUMN_REPEAT_STATE, state.mRepeatState.ordinal());
        values.put(StateTable.COLUMN_AUTOSAVE, autosave);
        values.put(StateTable.COLUMN_TITLE, title);
        values.put(StateTable.COLUMN_TRACKS, numberOfTracks);

        odysseyStateDB.insert(StateTable.TABLE_NAME, null, values);

        odysseyStateDB.setTransactionSuccessful();
        odysseyStateDB.endTransaction();

        odysseyStateDB.close();
    }

    /**
     * Return a state object for the given timestamp
     */
    public OdysseyServiceState getState(long timeStamp) {

        SQLiteDatabase odysseyStateDB = getReadableDatabase();

        OdysseyServiceState state = new OdysseyServiceState();

        Cursor cursor = odysseyStateDB.query(StateTable.TABLE_NAME, projectionState, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)}, "", "", "");

        if (cursor.moveToFirst()) {

            state.mTrackNumber = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKNUMBER));
            state.mTrackPosition = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKPOSITION));
            state.mRandomState = PlaybackService.RANDOMSTATE.values()[cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_RANDOM_STATE))];
            state.mRepeatState = PlaybackService.REPEATSTATE.values()[cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_REPEAT_STATE))];
        }

        cursor.close();

        odysseyStateDB.close();

        return state;
    }

    /**
     * Return the most recent state object
     */
    public OdysseyServiceState getState() {

        SQLiteDatabase odysseyStateDB = getReadableDatabase();

        OdysseyServiceState state = new OdysseyServiceState();

        Cursor cursor = odysseyStateDB.query(StateTable.TABLE_NAME, projectionState, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC", "1");

        if (cursor.moveToFirst()) {

            state.mTrackNumber = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKNUMBER));
            state.mTrackPosition = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKPOSITION));
            state.mRandomState = PlaybackService.RANDOMSTATE.values()[cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_RANDOM_STATE))];
            state.mRepeatState = PlaybackService.REPEATSTATE.values()[cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_REPEAT_STATE))];
        }

        cursor.close();

        odysseyStateDB.close();

        return state;
    }

    /**
     * Return all custom saved states as Bookmark objects
     */
    public List<BookmarkModel> getBookmarks() {

        SQLiteDatabase odysseyStateDB = getReadableDatabase();

        ArrayList<BookmarkModel> bookmarks = new ArrayList<>();

        String whereVal[] = {"0"};

        String where = StateTable.COLUMN_AUTOSAVE + "=?";

        Cursor bookmarkCursor = odysseyStateDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TITLE, StateTable.COLUMN_TRACKS, StateTable.COLUMN_AUTOSAVE},
                where, whereVal, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

        if (bookmarkCursor != null) {

            if (bookmarkCursor.moveToFirst()) {
                do {
                    long timeStamp = bookmarkCursor.getLong(bookmarkCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));
                    String title = bookmarkCursor.getString(bookmarkCursor.getColumnIndex(StateTable.COLUMN_TITLE));
                    int numberOfTracks = bookmarkCursor.getInt(bookmarkCursor.getColumnIndex(StateTable.COLUMN_TRACKS));

                    bookmarks.add(new BookmarkModel(timeStamp, title, numberOfTracks));

                } while (bookmarkCursor.moveToNext());
            }

            bookmarkCursor.close();
        }

        odysseyStateDB.close();

        return bookmarks;
    }

    /**
     * Remove the state from the database related to the given timestamp
     */
    public void removeState(long timestamp) {

        SQLiteDatabase odysseyStateDB = getWritableDatabase();

        // delete playlist
        odysseyStateDB.delete(StateTracksTable.TABLE_NAME, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});

        // delete state
        odysseyStateDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});

        odysseyStateDB.close();
    }

    /**
     * Remove all states marked as auto generated, including their related tracks
     */
    private void clearAutoSaveState() {

        SQLiteDatabase odysseyStateDB = getWritableDatabase();

        Cursor stateCursor = odysseyStateDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_AUTOSAVE}, StateTable.COLUMN_AUTOSAVE + "=?", new String[]{"1"},
                "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

        if (stateCursor.moveToFirst()) {
            do {
                long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

                // delete playlist
                odysseyStateDB.delete(StateTracksTable.TABLE_NAME, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});

                // delete state
                odysseyStateDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});
            } while (stateCursor.moveToNext());
        }

        stateCursor.close();

        odysseyStateDB.close();
    }
}
