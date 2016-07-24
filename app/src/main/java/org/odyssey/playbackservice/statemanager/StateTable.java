package org.odyssey.playbackservice.statemanager;

import android.database.sqlite.SQLiteDatabase;

public class StateTable {

    /**
     * The name of the table.
     */
    public static final String TABLE_NAME = "odyssey_states";

    /**
     * Name of the column that holds the timestamp related to the state
     */
    public static final String COLUMN_BOOKMARK_TIMESTAMP = "bookmark_timestamp";

    /**
     * Name of the column that holds the number of the latest played track
     */
    public static final String COLUMN_TRACKNUMBER = "tracknumber";

    /**
     * Name of the column that holds the position in the latest played track
     */
    public static final String COLUMN_TRACKPOSITION = "trackposition";

    /**
     * Name of the column that holds random state
     */
    public static final String COLUMN_RANDOM_STATE = "randomstate";

    /**
     * Name of the column that holds the repeat state
     */
    public static final String COLUMN_REPEAT_STATE = "repeatstate";

    /**
     * Name of the column that holds the autosave value
     */
    public static final String COLUMN_AUTOSAVE = "autosave";

    /**
     * Name of the column that holds the title of the state
     */
    public static final String COLUMN_TITLE = "title";

    /**
     * Name of the column that holds the number of tracks of the state
     */
    public static final String COLUMN_TRACKS = "tracks";

    /**
     * Database creation SQL statement
     */
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" + COLUMN_BOOKMARK_TIMESTAMP
            + " integer primary key," + COLUMN_TRACKNUMBER + " integer," + COLUMN_TRACKPOSITION + " integer," + COLUMN_RANDOM_STATE + " integer, "
            + COLUMN_REPEAT_STATE + " integer," + COLUMN_AUTOSAVE + " integer," + COLUMN_TITLE + " text," + COLUMN_TRACKS + " integer" +");";

    public static void onCreate(SQLiteDatabase database) {
        // create new table
        database.execSQL(DATABASE_CREATE);
    }
}
