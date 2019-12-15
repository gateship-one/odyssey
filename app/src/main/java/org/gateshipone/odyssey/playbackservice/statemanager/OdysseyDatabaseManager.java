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

package org.gateshipone.odyssey.playbackservice.statemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.gateshipone.odyssey.database.AndroidDBStateTracksTable;
import org.gateshipone.odyssey.database.MusicDatabaseFactory;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.OdysseyServiceState;
import org.gateshipone.odyssey.playbackservice.PlaybackService;

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
    private static final int DATABASE_VERSION = 21;

    private static OdysseyDatabaseManager mInstance;

    private Context mContext;
    /**
     * Array of returned columns from the State table
     */
    private String[] projectionState = {StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TRACKNUMBER, StateTable.COLUMN_TRACKPOSITION, StateTable.COLUMN_RANDOM_STATE, StateTable.COLUMN_REPEAT_STATE};

    private OdysseyDatabaseManager(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    public static synchronized OdysseyDatabaseManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new OdysseyDatabaseManager(context);
        }
        return mInstance;
    }


    /**
     * Called when the database is created for the first time.
     * This method creates the StateTracks and the State table
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        AndroidDBStateTracksTable.onCreate(db);
        StateTable.onCreate(db);
    }

    /**
     * Called when the database needs to be upgraded.
     * This method is currently not implemented.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // FIXME if database schema change provide update path here
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

        final long stateTimeStamp = System.currentTimeMillis();

        final ContentValues values = new ContentValues();

        final SQLiteDatabase odysseyStateDB = getWritableDatabase();

        odysseyStateDB.beginTransaction();

        if (autosave) {
            // delete previous auto saved states if this save is an auto generated save
            final Cursor stateCursor = odysseyStateDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_AUTOSAVE}, StateTable.COLUMN_AUTOSAVE + "=?", new String[]{"1"},
                    "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

            if (stateCursor.moveToFirst()) {
                do {
                    final long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

                    // delete playlist
                    MusicDatabaseFactory.getDatabase(mContext).deleteTrackList(timeStamp, odysseyStateDB);

                    // delete state
                    odysseyStateDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});
                } while (stateCursor.moveToNext());
            }

            stateCursor.close();
        } else {
            // delete the state with the same name from the database if exists
            final Cursor stateCursor = odysseyStateDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TITLE}, StateTable.COLUMN_TITLE + "=?", new String[]{title},
                    "", "", "");

            if (stateCursor.moveToFirst()) {
                final long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

                // delete playlist
                MusicDatabaseFactory.getDatabase(mContext).deleteTrackList(timeStamp, odysseyStateDB);

                // delete state
                odysseyStateDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});
            }

            stateCursor.close();
        }

        // save the current state
        values.clear();

        // set state parameters
        values.put(StateTable.COLUMN_BOOKMARK_TIMESTAMP, stateTimeStamp);
        values.put(StateTable.COLUMN_TRACKNUMBER, state.mTrackNumber);
        values.put(StateTable.COLUMN_TRACKPOSITION, state.mTrackPosition);
        values.put(StateTable.COLUMN_RANDOM_STATE, state.mRandomState.ordinal());
        values.put(StateTable.COLUMN_REPEAT_STATE, state.mRepeatState.ordinal());
        values.put(StateTable.COLUMN_AUTOSAVE, autosave);
        values.put(StateTable.COLUMN_TITLE, title);
        values.put(StateTable.COLUMN_TRACKS, playList.size());

        odysseyStateDB.insert(StateTable.TABLE_NAME, null, values);

        odysseyStateDB.setTransactionSuccessful();
        odysseyStateDB.endTransaction();

        // save the playlist
        MusicDatabaseFactory.getDatabase(mContext).saveTrackList(playList, stateTimeStamp, odysseyStateDB);

        // close the connection
        odysseyStateDB.close();
    }

    /**
     * Return the playlist for the given timestamp
     */
    public List<TrackModel> readPlaylist(long timeStamp) {
        final SQLiteDatabase db = getReadableDatabase();
        List<TrackModel> playlist = MusicDatabaseFactory.getDatabase(mContext).loadTrackList(timeStamp, db);
        db.close();
        return playlist;
    }

    /**
     * Returns the playlist for the most recent timestamp
     */
    public List<TrackModel> readPlaylist() {

        final SQLiteDatabase odysseyStateDB = getReadableDatabase();

        List<TrackModel> playList = null;

        // query the most recent timestamp
        final Cursor stateCursor = odysseyStateDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP}, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC", "1");

        if (stateCursor.moveToFirst()) {
            final long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

           playList = MusicDatabaseFactory.getDatabase(mContext).loadTrackList(timeStamp, odysseyStateDB);
        }

        stateCursor.close();

        odysseyStateDB.close();

        return playList == null ? new ArrayList<>() : playList;
    }

    /**
     * Return a state object for the given timestamp
     */
    public OdysseyServiceState getState(final long timeStamp) {

        final SQLiteDatabase odysseyStateDB = getReadableDatabase();

        final OdysseyServiceState state = new OdysseyServiceState();

        final Cursor cursor = odysseyStateDB.query(StateTable.TABLE_NAME, projectionState, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)}, "", "", "", "1");

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

        final SQLiteDatabase odysseyStateDB = getReadableDatabase();

        final OdysseyServiceState state = new OdysseyServiceState();

        final Cursor cursor = odysseyStateDB.query(StateTable.TABLE_NAME, projectionState, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC", "1");

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

        final SQLiteDatabase odysseyStateDB = getReadableDatabase();

        final ArrayList<BookmarkModel> bookmarks = new ArrayList<>();

        final String[] whereVal = {"0"};

        final String where = StateTable.COLUMN_AUTOSAVE + "=?";

        final Cursor bookmarkCursor = odysseyStateDB.query(StateTable.TABLE_NAME, new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TITLE, StateTable.COLUMN_TRACKS, StateTable.COLUMN_AUTOSAVE},
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
    public void removeState(final long timestamp) {

        final SQLiteDatabase odysseyStateDB = getWritableDatabase();

        odysseyStateDB.beginTransaction();

        // delete playlist
        odysseyStateDB.delete(AndroidDBStateTracksTable.TABLE_NAME, AndroidDBStateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});

        // delete state
        odysseyStateDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});

        odysseyStateDB.setTransactionSuccessful();
        odysseyStateDB.endTransaction();

        odysseyStateDB.close();
    }
}
