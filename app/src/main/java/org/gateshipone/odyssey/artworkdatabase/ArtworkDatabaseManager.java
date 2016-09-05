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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

public class ArtworkDatabaseManager extends SQLiteOpenHelper {

    /**
     * The name of the database
     */
    private static final String DATABASE_NAME = "OdysseyArtworkDB";

    /**
     * The version of the database
     */
    private static final int DATABASE_VERSION = BuildConfig.VERSION_CODE;

    private Context mContext;

    private static ArtworkDatabaseManager mInstance;

    private ArtworkDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized ArtworkDatabaseManager getInstance(Context context) {
        if ( null == mInstance ) {
            mInstance = new ArtworkDatabaseManager(context);
            ArtworkDatabaseManager.mInstance.mContext = context;
        }
        return mInstance;
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

    public synchronized byte[] getAlbumImage(long id) {
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

    public synchronized byte[] getArtistImage(long id) {
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

    public synchronized byte[] getArtistImage(String artistName) {
        SQLiteDatabase database = getReadableDatabase();

        String selection = ArtistArtTable.COLUMN_ARTIST_NAME + "=?";


        Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_NAME, ArtistArtTable.COLUMN_IMAGE_DATA},
                selection, new String[]{artistName}, null, null, null);

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

    public synchronized void insertArtistImage(ArtistModel artist, byte[] image) {
        SQLiteDatabase database = getWritableDatabase();

        long artistID = artist.getArtistID();

        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist.getArtistName(), mContext );
        }

        ContentValues values = new ContentValues();

        values.put(ArtistArtTable.COLUMN_ARTIST_ID, artistID);
        values.put(ArtistArtTable.COLUMN_ARTIST_MBID, artist.getMBID());
        values.put(ArtistArtTable.COLUMN_ARTIST_NAME, artist.getArtistName());
        values.put(ArtistArtTable.COLUMN_IMAGE_DATA, image);

        database.replace(ArtistArtTable.TABLE_NAME,"", values);

        database.close();
    }

}
