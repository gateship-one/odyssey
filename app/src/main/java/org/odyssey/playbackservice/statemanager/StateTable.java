package org.odyssey.playbackservice.statemanager;

import android.database.sqlite.SQLiteDatabase;

public class StateTable {

    // State table
    public static final String TABLE_NAME = "odyssey_currentplaylist_state";

    public static final String COLUMN_BOOKMARK_TIMESTAMP = "bookmark_timestamp";
    public static final String COLUMN_TRACKNUMBER = "tracknumber";
    public static final String COLUMN_TRACKPOSITION = "trackposition";
    public static final String COLUMN_RANDOM_STATE = "randomstate";
    public static final String COLUMN_REPEAT_STATE = "repeatstate";
    public static final String COLUMN_AUTOSAVE = "autosave";

    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" + COLUMN_BOOKMARK_TIMESTAMP
            + " integer primary key," + COLUMN_TRACKNUMBER + " integer," + COLUMN_TRACKPOSITION + " integer," + COLUMN_RANDOM_STATE + " integer, "
            + COLUMN_REPEAT_STATE + " integer," + COLUMN_AUTOSAVE + " integer" +");";

    public static void onCreate(SQLiteDatabase database) {
        // create new table
        database.execSQL(DATABASE_CREATE);
    }
}
