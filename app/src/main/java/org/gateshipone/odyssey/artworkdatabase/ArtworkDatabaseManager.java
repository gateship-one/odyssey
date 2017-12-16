/*
 * Copyright (C) 2017 Team Gateship-One
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

package org.gateshipone.odyssey.artworkdatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.gateshipone.odyssey.models.AlbumModel;
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
    private static final int DATABASE_VERSION = 21;

    private static ArtworkDatabaseManager mInstance;

    private ArtworkDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized ArtworkDatabaseManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ArtworkDatabaseManager(context);
        }
        return mInstance;
    }


    /**
     * Creates the database tables if they are not already existing
     *
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        AlbumArtTable.createTable(db);
        ArtistArtTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //FIXME
    }

    /**
     * Tries to fetch an image for the album with the given id (android album id).
     *
     * @param id Android MediaColumns album_id.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImage(long id) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = AlbumArtTable.COLUMN_ALBUM_ID + "=?";


        Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_ID, AlbumArtTable.COLUMN_IMAGE_DATA, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{String.valueOf(id)}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Tries to fetch an image for the artist with the given id (android artist id).
     *
     * @param id Android MediaColumns artist_id.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getArtistImage(long id) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = ArtistArtTable.COLUMN_ARTIST_ID + "=?";


        Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_ID, ArtistArtTable.COLUMN_IMAGE_DATA, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{String.valueOf(id)}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Tries to fetch an image for the album with the given name. This is useful if artist_id is not set
     *
     * @param artistName The name of the artist to search for.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getArtistImage(String artistName) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = ArtistArtTable.COLUMN_ARTIST_NAME + "=?";


        Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_NAME, ArtistArtTable.COLUMN_IMAGE_DATA, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{artistName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Inserts the given byte[] image to the artists table.
     *
     * @param artist Artist for the associated image byte[].
     * @param image  byte[] containing the raw image that was downloaded. This can be null in which case
     *               the database entry will have the not_found flag set.
     */
    public synchronized void insertArtistImage(ArtistModel artist, byte[] image, Context context) {
        SQLiteDatabase database = getWritableDatabase();

        long artistID = artist.getArtistID();

        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist.getArtistName(), context);
        }

        ContentValues values = new ContentValues();

        values.put(ArtistArtTable.COLUMN_ARTIST_ID, artistID);
        values.put(ArtistArtTable.COLUMN_ARTIST_MBID, artist.getMBID());
        values.put(ArtistArtTable.COLUMN_ARTIST_NAME, artist.getArtistName());
        values.put(ArtistArtTable.COLUMN_IMAGE_DATA, image);

        // If null was given as byte[] set the not_found flag for this entry.
        values.put(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND, image == null ? 1 : 0);

        database.replace(ArtistArtTable.TABLE_NAME, "", values);

        database.close();
    }


    /**
     * Tries to fetch an image for the album with the given name. This can result in wrong results for e.g. "Greatest Hits"
     *
     * @param albumName The name of the album to search for.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImage(String albumName) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=?";


        Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_NAME, AlbumArtTable.COLUMN_IMAGE_DATA, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{albumName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Inserts the given byte[] image to the albums table.
     *
     * @param album Album for the associated image byte[].
     * @param image byte[] containing the raw image that was downloaded. This can be null in which case
     *              the database entry will have the not_found flag set.
     */
    public synchronized void insertAlbumImage(AlbumModel album, byte[] image) {
        SQLiteDatabase database = getWritableDatabase();

        String albumID = String.valueOf(album.getAlbumID());

        ContentValues values = new ContentValues();

        values.put(AlbumArtTable.COLUMN_ALBUM_ID, albumID);
        values.put(AlbumArtTable.COLUMN_ALBUM_MBID, album.getMBID());
        values.put(AlbumArtTable.COLUMN_ALBUM_NAME, album.getAlbumName());
        values.put(AlbumArtTable.COLUMN_ARTIST_NAME, album.getArtistName());
        values.put(AlbumArtTable.COLUMN_IMAGE_DATA, image);

        // If null was given as byte[] set the not_found flag for this entry.
        values.put(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND, image == null ? 1 : 0);

        database.replace(AlbumArtTable.TABLE_NAME, "", values);

        database.close();
    }

    /**
     * Removes all lines from the artists table
     */
    public synchronized void clearArtistImages() {
        SQLiteDatabase database = getWritableDatabase();

        database.delete(ArtistArtTable.TABLE_NAME, null, null);

        database.close();
    }

    /**
     * Removes all lines from the albums table
     */
    public synchronized void clearAlbumImages() {
        SQLiteDatabase database = getWritableDatabase();

        database.delete(AlbumArtTable.TABLE_NAME, null, null);

        database.close();
    }

    public synchronized void clearBlockedArtistImages() {
        SQLiteDatabase database = getWritableDatabase();

        String where = ArtistArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        String whereArgs[] = {"1"};

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void clearBlockedAlbumImages() {
        SQLiteDatabase database = getWritableDatabase();

        String where = AlbumArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        String whereArgs[] = {"1"};

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void removeArtistImage(ArtistModel artist) {
        SQLiteDatabase database = getWritableDatabase();

        String where = ArtistArtTable.COLUMN_ARTIST_ID + "=? OR " +ArtistArtTable.COLUMN_ARTIST_NAME + "=?";
        String whereArgs[] = {String.valueOf(artist.getArtistID()), artist.getArtistName()};

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void removeAlbumImage(AlbumModel album) {
        SQLiteDatabase database = getWritableDatabase();

        String where = AlbumArtTable.COLUMN_ALBUM_ID+ "=?";
        String whereArgs[] = {String.valueOf(album.getAlbumID())};

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

}
