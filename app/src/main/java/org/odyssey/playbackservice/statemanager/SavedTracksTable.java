package org.odyssey.playbackservice.statemanager;

import android.database.sqlite.SQLiteDatabase;

public class SavedTracksTable {
    // SavedTracks table
    public static final String TABLE_NAME = "trackitem";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TRACKNUMBER = "tracknumber";
    public static final String COLUMN_TRACKTITLE = "title";
    public static final String COLUMN_TRACKALBUM = "album";
    public static final String COLUMN_TRACKALBUMKEY = "albumkey";
    public static final String COLUMN_TRACKDURATION = "duration";
    public static final String COLUMN_TRACKARTIST = "artist";
    public static final String COLUMN_TRACKURL = "url";

    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" + COLUMN_ID + " integer primary key autoincrement, " + COLUMN_TRACKNUMBER + " int, " + COLUMN_TRACKTITLE + " text," + COLUMN_TRACKALBUM + " text,"
            + COLUMN_TRACKALBUMKEY + " text," + COLUMN_TRACKDURATION + " int," + COLUMN_TRACKARTIST + " text," + COLUMN_TRACKURL + " text" + ");";

    public static void onCreate(SQLiteDatabase database) {
        // create new table
        database.execSQL(DATABASE_CREATE);
    }
}
