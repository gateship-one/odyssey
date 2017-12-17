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
import org.gateshipone.odyssey.utils.FileUtils;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ArtworkDatabaseManager extends SQLiteOpenHelper {

    /**
     * The name of the database
     */
    private static final String DATABASE_NAME = "OdysseyArtworkDB";

    /**
     * The version of the database
     */
    private static final int DATABASE_VERSION = 22;

    private static ArtworkDatabaseManager mInstance;

    private static final String DIRECTORY_ALBUM_IMAGES = "albumArt";

    private static final String DIRECTORY_ARTIST_IMAGES = "artistArt";

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
        if (newVersion == 22) {
            AlbumArtTable.dropTable(db);
            ArtistArtTable.dropTable(db);
            onCreate(db);
        }
    }

    /**
     * Tries to fetch an image for the album with the given id (android album id).
     *
     * @param id Android MediaColumns album_id.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImage(final Context context, long id) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = AlbumArtTable.COLUMN_ALBUM_ID + "=?";


        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_ID, AlbumArtTable.COLUMN_IMAGE_FILE_PATH, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{String.valueOf(id)}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String imageFileName = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            final byte[] image = FileUtils.readImageFile(context, imageFileName, DIRECTORY_ALBUM_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized byte[] getArtistImage(final Context context, long id) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = ArtistArtTable.COLUMN_ARTIST_ID + "=?";


        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_ID, ArtistArtTable.COLUMN_IMAGE_FILE_PATH, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{String.valueOf(id)}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String imageFileName = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            final byte[] image = FileUtils.readImageFile(context, imageFileName, DIRECTORY_ARTIST_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized byte[] getArtistImage(final Context context, String artistName) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = ArtistArtTable.COLUMN_ARTIST_NAME + "=?";

        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_NAME, ArtistArtTable.COLUMN_IMAGE_FILE_PATH, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{artistName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String imageFileName = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            final byte[] image = FileUtils.readImageFile(context, imageFileName, DIRECTORY_ARTIST_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized void insertArtistImage(final Context context, final ArtistModel artist, final byte[] image) {
        final SQLiteDatabase database = getWritableDatabase();

        long artistID = artist.getArtistID();
        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist.getArtistName(), context);
        }

        final String artistIDString = String.valueOf(artistID);
        final String artistMBID = artist.getMBID();
        final String artistName = artist.getArtistName();

        String imageFileName = null;
        if (image != null) {
            try {
                imageFileName = FileUtils.createSHA256HashForString(artistIDString, artistMBID, artistName) + ".jpg";
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return;
            }

            try {
                FileUtils.saveImageFile(context, imageFileName, DIRECTORY_ARTIST_IMAGES, image);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        final ContentValues values = new ContentValues();
        values.put(ArtistArtTable.COLUMN_ARTIST_ID, artistID);
        values.put(ArtistArtTable.COLUMN_ARTIST_MBID, artist.getMBID());
        values.put(ArtistArtTable.COLUMN_ARTIST_NAME, artist.getArtistName());
        values.put(ArtistArtTable.COLUMN_IMAGE_FILE_PATH, imageFileName);

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
    public synchronized byte[] getAlbumImage(final Context context, final String albumName) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=?";

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_NAME, AlbumArtTable.COLUMN_IMAGE_FILE_PATH, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{albumName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String imageFileName = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            final byte[] image = FileUtils.readImageFile(context, imageFileName, DIRECTORY_ALBUM_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized void insertAlbumImage(final Context context, final AlbumModel album, final byte[] image) {
        final SQLiteDatabase database = getWritableDatabase();

        final String albumID = String.valueOf(album.getAlbumID());
        final String albumMBID = album.getMBID();
        final String albumName = album.getAlbumName();
        final String albumArtistName = album.getArtistName();

        String imageFileName = null;
        if (image != null) {
            try {
                imageFileName = FileUtils.createSHA256HashForString(albumID, albumMBID, albumName, albumArtistName) + ".jpg";
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return;
            }

            try {
                FileUtils.saveImageFile(context, imageFileName, DIRECTORY_ALBUM_IMAGES, image);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        final ContentValues values = new ContentValues();
        values.put(AlbumArtTable.COLUMN_ALBUM_ID, albumID);
        values.put(AlbumArtTable.COLUMN_ALBUM_MBID, albumMBID);
        values.put(AlbumArtTable.COLUMN_ALBUM_NAME, album.getAlbumName());
        values.put(AlbumArtTable.COLUMN_ARTIST_NAME, albumName);
        values.put(AlbumArtTable.COLUMN_IMAGE_FILE_PATH, imageFileName);

        // If null was given as byte[] set the not_found flag for this entry.
        values.put(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND, image == null ? 1 : 0);

        database.replace(AlbumArtTable.TABLE_NAME, "", values);

        database.close();
    }

    /**
     * Removes all lines from the artists table
     */
    public synchronized void clearArtistImages(final Context context) {
        final SQLiteDatabase database = getWritableDatabase();

        database.delete(ArtistArtTable.TABLE_NAME, null, null);

        database.close();

        FileUtils.removeImageDirectory(context, DIRECTORY_ARTIST_IMAGES);
    }

    /**
     * Removes all lines from the albums table
     */
    public synchronized void clearAlbumImages(final Context context) {
        final SQLiteDatabase database = getWritableDatabase();

        database.delete(AlbumArtTable.TABLE_NAME, null, null);

        database.close();

        FileUtils.removeImageDirectory(context, DIRECTORY_ALBUM_IMAGES);
    }

    public synchronized void clearBlockedArtistImages() {
        final SQLiteDatabase database = getWritableDatabase();

        final String where = ArtistArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        final String whereArgs[] = {"1"};

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void clearBlockedAlbumImages() {
        final SQLiteDatabase database = getWritableDatabase();

        final String where = AlbumArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        final String whereArgs[] = {"1"};

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void removeArtistImage(final Context context, final ArtistModel artist) {
        final SQLiteDatabase database = getWritableDatabase();

        final String where = ArtistArtTable.COLUMN_ARTIST_ID + "=? OR " + ArtistArtTable.COLUMN_ARTIST_NAME + "=?";
        final String whereArgs[] = {String.valueOf(artist.getArtistID()), artist.getArtistName()};

        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_IMAGE_FILE_PATH},
                where, whereArgs, null, null, null);

        if (requestCursor.moveToFirst()) {

            final String imageFileName = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();

            FileUtils.removeImageFile(context, imageFileName, DIRECTORY_ARTIST_IMAGES);
        }

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void removeAlbumImage(final Context context, final AlbumModel album) {
        final SQLiteDatabase database = getWritableDatabase();

        final String where = AlbumArtTable.COLUMN_ALBUM_ID + "=?";
        final String whereArgs[] = {String.valueOf(album.getAlbumID())};

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_IMAGE_FILE_PATH},
                where, whereArgs, null, null, null);

        if (requestCursor.moveToFirst()) {

            final String imageFileName = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();

            FileUtils.removeImageFile(context, imageFileName, DIRECTORY_ARTIST_IMAGES);
        }

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }
}
