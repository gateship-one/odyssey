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
import android.net.Uri;
import android.util.Log;

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.models.PlaylistModel;
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
    private static final int DATABASE_VERSION = 22;

    private static OdysseyDatabaseManager mInstance;

    /**
     * Array of returned columns from the StateTracks table
     */
    private String[] projectionTrackModels = {
            StateTracksTable.COLUMN_TRACKNUMBER,
            StateTracksTable.COLUMN_TRACKTITLE,
            StateTracksTable.COLUMN_TRACKALBUM,
            StateTracksTable.COLUMN_TRACKALBUMKEY,
            StateTracksTable.COLUMN_TRACKDURATION,
            StateTracksTable.COLUMN_TRACKARTIST,
            StateTracksTable.COLUMN_TRACKURL,
            StateTracksTable.COLUMN_TRACKID
    };

    /**
     * Array of returned columns from the State table
     */
    private String[] projectionState = {
            StateTable.COLUMN_BOOKMARK_TIMESTAMP,
            StateTable.COLUMN_TRACKNUMBER,
            StateTable.COLUMN_TRACKPOSITION,
            StateTable.COLUMN_RANDOM_STATE,
            StateTable.COLUMN_REPEAT_STATE
    };

    /**
     * Array of returned columns from the Playlists table
     */
    private String[] projectionPlaylists = {
            PlaylistsTable.COLUMN_ID,
            PlaylistsTable.COLUMN_TITLE,
            PlaylistsTable.COLUMN_TRACKS
    };

    /**
     * Array of returned columns from the PlaylistTracks table
     */
    private String[] projectionPlaylistTracks = {
            PlaylistsTracksTable.COLUMN_ID,
            PlaylistsTracksTable.COLUMN_TRACK_NUMBER,
            PlaylistsTracksTable.COLUMN_TRACK_TITLE,
            PlaylistsTracksTable.COLUMN_TRACK_ALBUM,
            PlaylistsTracksTable.COLUMN_TRACK_ALBUMKEY,
            PlaylistsTracksTable.COLUMN_TRACK_DURATION,
            PlaylistsTracksTable.COLUMN_TRACK_ARTIST,
            PlaylistsTracksTable.COLUMN_TRACK_URL,
            PlaylistsTracksTable.COLUMN_TRACK_ID,
            PlaylistsTracksTable.COLUMN_PLAYLIST_ID
    };

    private OdysseyDatabaseManager(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
        StateTracksTable.onCreate(db);
        StateTable.onCreate(db);
        PlaylistsTracksTable.onCreate(db);
        PlaylistsTable.onCreate(db);
    }

    /**
     * Called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 22) {
            onCreate(db);
        }
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
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "save state");
        }

        final long stateTimeStamp = System.currentTimeMillis();

        final ContentValues values = new ContentValues();

        final SQLiteDatabase odysseyStateDB = getWritableDatabase();

        odysseyStateDB.beginTransaction();

        if (autosave) {
            // delete previous auto saved states if this save is an auto generated save
            final Cursor stateCursor = odysseyStateDB.query(
                    StateTable.TABLE_NAME,
                    new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_AUTOSAVE},
                    StateTable.COLUMN_AUTOSAVE + "=?",
                    new String[]{"1"},
                    "",
                    "",
                    StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

            if (stateCursor.moveToFirst()) {
                do {
                    final long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

                    // delete playlist
                    odysseyStateDB.delete(StateTracksTable.TABLE_NAME, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});

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
                odysseyStateDB.delete(StateTracksTable.TABLE_NAME, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});

                // delete state
                odysseyStateDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timeStamp)});
            }

            stateCursor.close();
        }

        // save the playlist
        for (TrackModel item : playList) {

            values.clear();

            // set TrackModel parameters
            values.put(StateTracksTable.COLUMN_TRACKTITLE, item.getTrackName());
            values.put(StateTracksTable.COLUMN_TRACKDURATION, item.getTrackDuration());
            values.put(StateTracksTable.COLUMN_TRACKNUMBER, item.getTrackNumber());
            values.put(StateTracksTable.COLUMN_TRACKARTIST, item.getTrackArtistName());
            values.put(StateTracksTable.COLUMN_TRACKALBUM, item.getTrackAlbumName());
            values.put(StateTracksTable.COLUMN_TRACKURL, item.getTrackUriString());
            values.put(StateTracksTable.COLUMN_TRACKALBUMKEY, item.getTrackAlbumKey());
            values.put(StateTracksTable.COLUMN_TRACKID, item.getTrackId());
            values.put(StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP, stateTimeStamp);

            odysseyStateDB.insert(StateTracksTable.TABLE_NAME, null, values);
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

        // close the connection
        odysseyStateDB.close();
    }

    /**
     * Return all tracks from a bookmark.
     *
     * @param timeStamp The timestamp which identifies the bookmark.
     * @return All tracks for the bookmark as list of {@link TrackModel}.
     */
    public List<TrackModel> readBookmarkTracks(long timeStamp) {

        final SQLiteDatabase odysseyStateDB = getReadableDatabase();

        final List<TrackModel> playList = new ArrayList<>();

        final Cursor cursor = odysseyStateDB.query(
                StateTracksTable.TABLE_NAME,
                projectionTrackModels,
                StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?",
                new String[]{Long.toString(timeStamp)},
                "",
                "",
                StateTracksTable.COLUMN_ID);

        if (cursor.moveToFirst()) {
            do {
                final String trackName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKTITLE));
                final long duration = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKDURATION));
                final int number = cursor.getInt(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKNUMBER));
                final String artistName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKARTIST));
                final String albumName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUM));
                final String url = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKURL));
                final String albumKey = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUMKEY));
                final long id = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKID));

                TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, Uri.parse(url), id);

                playList.add(item);

            } while (cursor.moveToNext());
        }

        cursor.close();

        odysseyStateDB.close();

        return playList;
    }

    /**
     * Return all tracks from the most recent bookmark
     *
     * @return All tracks for the bookmark as list of {@link TrackModel}.
     */
    public List<TrackModel> readBookmarkTracks() {

        final SQLiteDatabase odysseyStateDB = getReadableDatabase();

        final List<TrackModel> playList = new ArrayList<>();

        // query the most recent timestamp
        final Cursor stateCursor = odysseyStateDB.query(StateTable.TABLE_NAME,
                new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP},
                "",
                null,
                "",
                "",
                StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC",
                "1");

        if (stateCursor.moveToFirst()) {
            final long timeStamp = stateCursor.getLong(stateCursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

            // get the playlist tracks for the queried timestamp
            final Cursor cursor = odysseyStateDB.query(StateTracksTable.TABLE_NAME,
                    projectionTrackModels,
                    StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?",
                    new String[]{Long.toString(timeStamp)},
                    "",
                    "",
                    StateTracksTable.COLUMN_ID);

            if (cursor.moveToFirst()) {
                do {
                    final String trackName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKTITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKDURATION));
                    final int number = cursor.getInt(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKNUMBER));
                    final String artistName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKARTIST));
                    final String albumName = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUM));
                    final String url = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKURL));
                    final String albumKey = cursor.getString(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKALBUMKEY));
                    final long id = cursor.getLong(cursor.getColumnIndex(StateTracksTable.COLUMN_TRACKID));

                    TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, Uri.parse(url), id);

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
     * Return a state object for the given timestamp
     */
    public OdysseyServiceState getState(final long timeStamp) {

        final SQLiteDatabase odysseyStateDB = getReadableDatabase();

        final OdysseyServiceState state = new OdysseyServiceState();

        final Cursor cursor = odysseyStateDB.query(
                StateTable.TABLE_NAME,
                projectionState,
                StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?",
                new String[]{Long.toString(timeStamp)},
                "",
                "",
                "",
                "1");

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

        final Cursor cursor = odysseyStateDB.query(StateTable.TABLE_NAME,
                projectionState,
                "",
                null,
                "",
                "",
                StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC",
                "1");

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

        final Cursor bookmarkCursor = odysseyStateDB.query(
                StateTable.TABLE_NAME,
                new String[]{StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TITLE, StateTable.COLUMN_TRACKS, StateTable.COLUMN_AUTOSAVE},
                where,
                whereVal,
                "",
                "",
                StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

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
        odysseyStateDB.delete(StateTracksTable.TABLE_NAME, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});

        // delete state
        odysseyStateDB.delete(StateTable.TABLE_NAME, StateTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)});

        odysseyStateDB.setTransactionSuccessful();
        odysseyStateDB.endTransaction();

        odysseyStateDB.close();
    }

    /**
     * Method to save a list of {@link TrackModel} as a playlist in the database.
     *
     * @param playlistName The name for the playlist.
     * @param tracks       The list of {@link TrackModel} that should be part of the playlist.
     */
    public void savePlaylist(final String playlistName, final List<TrackModel> tracks) {
        final SQLiteDatabase odysseyDB = getWritableDatabase();

        final Cursor cursor = odysseyDB.query(
                PlaylistsTable.TABLE_NAME,
                projectionPlaylists,
                PlaylistsTable.COLUMN_TITLE + "=?",
                new String[]{playlistName},
                "",
                "",
                ""
        );

        if (cursor.moveToFirst()) {
            final long playlistId = cursor.getLong(cursor.getColumnIndex(PlaylistsTable.COLUMN_ID));

            int result = -1;

            // delete playlist
            result = odysseyDB.delete(PlaylistsTable.TABLE_NAME,
                    PlaylistsTable.COLUMN_ID + "=?",
                    new String[]{Long.toString(playlistId)}
            );

            if (result > 0) {
                // delete tracks only if playlist was removed
                odysseyDB.delete(PlaylistsTracksTable.TABLE_NAME,
                        PlaylistsTracksTable.COLUMN_PLAYLIST_ID + "=?",
                        new String[]{Long.toString(playlistId)}
                );
            }
        }

        cursor.close();

        odysseyDB.beginTransaction();

        // create new playlist
        final ContentValues values = new ContentValues();
        values.put(PlaylistsTable.COLUMN_TITLE, playlistName);
        values.put(PlaylistsTable.COLUMN_TRACKS, tracks.size());

        final long playlistId = odysseyDB.insert(PlaylistsTable.TABLE_NAME, null, values);

        odysseyDB.setTransactionSuccessful();
        odysseyDB.endTransaction();

        odysseyDB.beginTransaction();

        // add tracks
        if (playlistId != -1) {
            // fix this if tracks should be appended
            int index = 1;

            for (TrackModel track : tracks) {
                values.clear();

                // set TrackModel parameters
                values.put(PlaylistsTracksTable.COLUMN_TRACK_TITLE, track.getTrackName());
                values.put(PlaylistsTracksTable.COLUMN_TRACK_DURATION, track.getTrackDuration());
                values.put(PlaylistsTracksTable.COLUMN_TRACK_NUMBER, track.getTrackNumber());
                values.put(PlaylistsTracksTable.COLUMN_TRACK_ARTIST, track.getTrackArtistName());
                values.put(PlaylistsTracksTable.COLUMN_TRACK_ALBUM, track.getTrackAlbumName());
                values.put(PlaylistsTracksTable.COLUMN_TRACK_URL, track.getTrackUriString());
                values.put(PlaylistsTracksTable.COLUMN_TRACK_ALBUMKEY, track.getTrackAlbumKey());
                values.put(PlaylistsTracksTable.COLUMN_TRACK_ID, track.getTrackId());
                values.put(PlaylistsTracksTable.COLUMN_PLAYLIST_ID, playlistId);
                values.put(PlaylistsTracksTable.COLUMN_PLAYLIST_POSITION, index);

                odysseyDB.insert(PlaylistsTracksTable.TABLE_NAME, null, values);

                index++;
            }
        }

        odysseyDB.setTransactionSuccessful();
        odysseyDB.endTransaction();

        odysseyDB.close();
    }

    /**
     * Removes a playlist from the database.
     *
     * @param playlistId The id to identify the playlist that should be deleted.
     * @return True if a playlist was removed otherwise false.
     */
    public boolean removePlaylist(final long playlistId) {
        final SQLiteDatabase odysseyDB = getWritableDatabase();

        int result = -1;

        odysseyDB.beginTransaction();

        // delete playlist
        result = odysseyDB.delete(PlaylistsTable.TABLE_NAME,
                PlaylistsTable.COLUMN_ID + "=?",
                new String[]{Long.toString(playlistId)}
        );

        if (result > 0) {
            // delete tracks only if playlist was removed
            odysseyDB.delete(PlaylistsTracksTable.TABLE_NAME,
                    PlaylistsTracksTable.COLUMN_PLAYLIST_ID + "=?",
                    new String[]{Long.toString(playlistId)}
            );
        }

        odysseyDB.setTransactionSuccessful();
        odysseyDB.endTransaction();

        odysseyDB.close();

        return result > 0;
    }

    /**
     * Method to return all playlists that are stored in the database.
     *
     * @return The stored playlists as a list of {@link PlaylistModel}.
     */
    public List<PlaylistModel> getPlaylists() {
        final SQLiteDatabase odysseyDB = getReadableDatabase();

        final ArrayList<PlaylistModel> playlists = new ArrayList<>();

        final Cursor cursor = odysseyDB.query(PlaylistsTable.TABLE_NAME,
                projectionPlaylists,
                "",
                null,
                "",
                "",
                PlaylistsTable.COLUMN_TITLE
        );

        if (cursor != null) {

            if (cursor.moveToFirst()) {
                final int playlistTitleColumnIndex = cursor.getColumnIndex(PlaylistsTable.COLUMN_TITLE);
                final int playlistIDColumnIndex = cursor.getColumnIndex(PlaylistsTable.COLUMN_ID);

                do {
                    final String playlistTitle = cursor.getString(playlistTitleColumnIndex);
                    final long playlistID = cursor.getLong(playlistIDColumnIndex);

                    playlists.add(new PlaylistModel(playlistTitle, playlistID, PlaylistModel.PLAYLIST_TYPES.ODYSSEY_LOCAL));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        odysseyDB.close();

        return playlists;
    }

    /**
     * Returns all tracks that are stored for a playlist.
     *
     * @param playlistId The id to identify the playlist.
     * @return All playlist tracks as a list of {@link TrackModel}.
     */
    public List<TrackModel> getTracksForPlaylist(final long playlistId) {
        final SQLiteDatabase odysseyDB = getReadableDatabase();

        final List<TrackModel> tracks = new ArrayList<>();

        final Cursor cursor = odysseyDB.query(
                PlaylistsTracksTable.TABLE_NAME,
                projectionPlaylistTracks,
                PlaylistsTracksTable.COLUMN_PLAYLIST_ID + "=?",
                new String[]{Long.toString(playlistId)},
                "",
                "",
                PlaylistsTracksTable.COLUMN_PLAYLIST_POSITION
        );

        if (cursor.moveToFirst()) {
            do {
                final String trackName = cursor.getString(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_TITLE));
                final long duration = cursor.getLong(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_DURATION));
                final int number = cursor.getInt(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_NUMBER));
                final String artistName = cursor.getString(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_ARTIST));
                final String albumName = cursor.getString(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_ALBUM));
                final String url = cursor.getString(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_URL));
                final String albumKey = cursor.getString(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_ALBUMKEY));
                final long id = cursor.getLong(cursor.getColumnIndex(PlaylistsTracksTable.COLUMN_TRACK_ID));

                TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, Uri.parse(url), id);

                tracks.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();

        odysseyDB.close();

        return tracks;
    }

    /**
     * Method to remove a single track from a playlist.
     *
     * @param playlistId    The id of the playlist.
     * @param trackPosition The position of the track in the playlist.
     * @return True if the track was removed otherwise false.
     */
    public boolean removeTrackFromPlaylist(final long playlistId, final int trackPosition) {
        final SQLiteDatabase odysseyDB = getWritableDatabase();

        int result = -1;

        final String where = PlaylistsTracksTable.COLUMN_PLAYLIST_ID + "=? AND " + PlaylistsTracksTable.COLUMN_PLAYLIST_POSITION + "=?";
        final String[] whereVal = {Long.toString(playlistId), Integer.toString(trackPosition)};

        odysseyDB.beginTransaction();

        // delete tracks
        result = odysseyDB.delete(PlaylistsTracksTable.TABLE_NAME,
                where,
                whereVal
        );

        odysseyDB.setTransactionSuccessful();
        odysseyDB.endTransaction();

        odysseyDB.close();

        return result > 0;
    }
}
