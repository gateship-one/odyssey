package org.odyssey.playbackservice.statemanager;

import android.database.sqlite.SQLiteDatabase;

public class StateTable {

    // State table
    public static final String TABLE_NAME = "odyssey_settings";
    public static final String COLUMN_SETTINGSNAME = "key";
    public static final String COLUMN_SETTINGSVALUE = "value";

    public static final String TRACKNUMBER_ROW = "tracknumber";
    public static final String TRACKPOSITION_ROW = "trackposition";
    public static final String RANDOM_STATE_ROW = "randomstate";
    public static final String REPEAT_STATE_ROW = "repeatstate";

    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" + COLUMN_SETTINGSNAME + " text, " + COLUMN_SETTINGSVALUE + " text);";

    public static void onCreate(SQLiteDatabase database) {
        // create new table
        database.execSQL(DATABASE_CREATE);
    }
}
