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

package org.gateshipone.odyssey.playbackservice.storage;

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
     * Name of the column that holds the id of the track from mediastore
     */
    public static final String COLUMN_TRACKID = "trackid";

    /**
     * Name of the column that holds the timestamp related to the bookmark
     */
    public static final String COLUMN_BOOKMARK_TIMESTAMP = "bookmark_timestamp";

    /**
     * Database creation SQL statement
     */
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" +
            COLUMN_ID + " integer primary key autoincrement," +
            COLUMN_TRACKNUMBER + " integer," +
            COLUMN_TRACKTITLE + " text," +
            COLUMN_TRACKALBUM + " text," +
            COLUMN_TRACKALBUMKEY + " text," +
            COLUMN_TRACKDURATION + " integer," +
            COLUMN_TRACKARTIST + " text," +
            COLUMN_TRACKURL + " text," +
            COLUMN_TRACKID + " integer," +
            COLUMN_BOOKMARK_TIMESTAMP + " integer " +
            ");";

    public static void onCreate(SQLiteDatabase database) {
        // create new table
        database.execSQL(DATABASE_CREATE);
    }
}
