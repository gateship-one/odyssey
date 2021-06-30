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

public class PlaylistsTable {

    /**
     * The name of the table.
     */
    public static final String TABLE_NAME = "odyssey_playlists";

    /**
     * The name of the column that holds a unique id for each playlist.
     */
    public static final String COLUMN_ID = "_id";

    /**
     * The name of the column that holds the title of the playlist.
     */
    public static final String COLUMN_TITLE = "title";

    /**
     * The name of the column that holds the number tracks of the playlist.
     */
    public static final String COLUMN_TRACKS = "tracks";

    /**
     * Database creation SQL statement
     */
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" +
            COLUMN_ID + " integer primary key," +
            COLUMN_TITLE + " text," +
            COLUMN_TRACKS + " integer" +
            ");";

    private static final String DATABASE_DROP = "DROP TABLE if exists " + TABLE_NAME;

    static void createTable(final SQLiteDatabase database) {
        // Create table if not already existing
        database.execSQL(DATABASE_CREATE);
    }

    static void dropTable(final SQLiteDatabase database) {
        // drop table if already existing
        database.execSQL(DATABASE_DROP);
    }
}
