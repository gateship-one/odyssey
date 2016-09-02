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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.gateshipone.odyssey.BuildConfig;

public class ArtworkDatabaseManager extends SQLiteOpenHelper {

    /**
     * The name of the database
     */
    private static final String DATABASE_NAME = "OdysseyArtworkDB";

    /**
     * The version of the database
     */
    private static final int DATABASE_VERSION = BuildConfig.VERSION_CODE;

    public ArtworkDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        AlbumArtTable.createTable(db);
        ArtistArtTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //FIXME
    }

    public byte[] getAlbumImage(long id) {
        SQLiteDatabase database = getReadableDatabase();

        String selection = AlbumArtTable.COLUMN_ALBUM_ID + "=?";


        Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_ID, AlbumArtTable.COLUMN_IMAGE_DATA},
                selection, new String[]{String.valueOf(id)}, null, null, null);

        if (requestCursor.moveToFirst()) {
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        requestCursor.close();
        database.close();
        return null;
    }

    public byte[] getArtistImage(long id) {
        SQLiteDatabase database = getReadableDatabase();

        String selection = ArtistArtTable.COLUMN_ARTIST_ID + "=?";


        Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_ID, ArtistArtTable.COLUMN_IMAGE_DATA},
                selection, new String[]{String.valueOf(id)}, null, null, null);

        if (requestCursor.moveToFirst()) {
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        requestCursor.close();
        database.close();
        return null;
    }

}
