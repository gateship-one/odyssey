package org.odyssey.playbackservice.statemanager;

import android.database.sqlite.SQLiteDatabase;

public class StateTracksTable {

    /**
     * The name of the table.
     */
    public static final String TABLE_NAME = "odyssey_state_tracks";

    /**
     * Name of the column that holds a unique id for each track
     */
    public static final String COLUMN_ID = "_id";

    /**
     * Name of the column that holds the number of the track in the related album
     */
    public static final String COLUMN_TRACKNUMBER = "tracknumber";

    /**
     * Name of the column that holds the title of the track
     */
    public static final String COLUMN_TRACKTITLE = "title";

    /**
     * Name of the column that holds the album name of the track
     */
    public static final String COLUMN_TRACKALBUM = "album";

    /**
     * Name of the column that holds the album key of the track
     */
    public static final String COLUMN_TRACKALBUMKEY = "albumkey";

    /**
     * Name of the column that holds the duration of the track
     */
    public static final String COLUMN_TRACKDURATION = "duration";

    /**
     * Name of the column that holds the artist name of the track
     */
    public static final String COLUMN_TRACKARTIST = "artist";

    /**
     * Name of the column that holds the url of the track
     */
    public static final String COLUMN_TRACKURL = "url";

    /**
     * Name of the column that holds the id of the rack from mediastore
     */
    public static final String COLUMN_TRACKID = "trackid";

    /**
     * Name of the column that holds the timestamp related to the track
     */
    public static final String COLUMN_BOOKMARK_TIMESTAMP = "bookmark_timestamp";

    /**
     * Database creation SQL statement
     */
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" + COLUMN_ID
            + " integer primary key autoincrement," + COLUMN_TRACKNUMBER + " integer," + COLUMN_TRACKTITLE + " text," + COLUMN_TRACKALBUM + " text,"
            + COLUMN_TRACKALBUMKEY + " text," + COLUMN_TRACKDURATION + " integer," + COLUMN_TRACKARTIST + " text," + COLUMN_TRACKURL + " text,"
            + COLUMN_TRACKID + " integer," + COLUMN_BOOKMARK_TIMESTAMP + " integer " + ");";

    public static void onCreate(SQLiteDatabase database) {
        // create new table
        database.execSQL(DATABASE_CREATE);
    }
}
