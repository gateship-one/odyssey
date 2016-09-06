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

package org.gateshipone.odyssey.artworkdatabase;

import android.database.sqlite.SQLiteDatabase;

public class ArtistArtTable {
    public static final String TABLE_NAME = "odyssey_artist_artwork_items";

    public static final String COLUMN_ARTIST_NAME = "artist_name";

    public static final String COLUMN_ARTIST_MBID = "artist_mbid";

    public static final String COLUMN_ARTIST_ID = "artist_id";

    public static final String COLUMN_IMAGE_DATA = "artist_image";

    public static final String COLUMN_IMAGE_NOT_FOUND = "image_not_found";

    private static final String DATABASE_CREATE = "CREATE TABLE if not exists " + TABLE_NAME + " (" +
            COLUMN_ARTIST_NAME + " text," + COLUMN_ARTIST_MBID + " text," + COLUMN_ARTIST_ID + " integer primary key," +
            COLUMN_IMAGE_NOT_FOUND + " integer," +
            COLUMN_IMAGE_DATA + " blob" +");";

    public static void createTable(SQLiteDatabase database) {
        // Create table if not already existing
        database.execSQL(DATABASE_CREATE);
    }
}
