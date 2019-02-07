/*
 * Copyright (C) 2019 Team Gateship-One
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

package org.gateshipone.odyssey.artwork.storage;

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
     * @param db The {@link SQLiteDatabase} instance that will be used to create the tables.
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
     * Tries to fetch an image for the album, by id (android album id), by album name and artist name or only by album name.
     *
     * @param album The album to search for.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized String getAlbumImage(final Context context, final AlbumModel album) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final long albumId = album.getAlbumID();
        final String albumName = album.getAlbumName();
        final String artistName = album.getArtistName();

        String selection;
        String selectionArguments[];

        if (albumId != -1) {
            selection = AlbumArtTable.COLUMN_ALBUM_ID + "=?";
            selectionArguments = new String[]{String.valueOf(albumId)};
        } else if (!artistName.isEmpty()) {
            selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=? AND " + AlbumArtTable.COLUMN_ARTIST_NAME + "=?";
            selectionArguments = new String[]{albumName, artistName};
        } else {
            selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=?";
            selectionArguments = new String[]{albumName};
        }

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_IMAGE_FILE_PATH, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, selectionArguments, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            return FileUtils.getFullArtworkFilePath(context, artworkFilename, DIRECTORY_ALBUM_IMAGES);
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Tries to fetch an image for the artist, by id (android artist id) or by the artist name.
     *
     * @param artist The artist to search for.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not found and it was not searched for before.
     */
    public synchronized String getArtistImage(final Context context, ArtistModel artist) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String artistName = artist.getArtistName();

        long artistId = artist.getArtistID();
        if (artistId == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistId = MusicLibraryHelper.getArtistIDFromName(artistName, context);
        }

        String selection;
        String selectionArguments[];

        if (artistId != -1) {
            selection = ArtistArtTable.COLUMN_ARTIST_ID + "=?";
            selectionArguments = new String[]{String.valueOf(artistId)};
        } else {
            selection = AlbumArtTable.COLUMN_ARTIST_NAME + "=?";
            selectionArguments = new String[]{artistName};
        }

        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_IMAGE_FILE_PATH, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, selectionArguments, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }

            // get the filename for the image
            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            return FileUtils.getFullArtworkFilePath(context, artworkFilename, DIRECTORY_ARTIST_IMAGES);
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Saves the given artist byte[] image.
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

        String artworkFilename = null;
        if (image != null) {
            try {
                artworkFilename = FileUtils.createSHA256HashForString(artistIDString, artistMBID, artistName) + ".jpg";
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return;
            }

            try {
                FileUtils.saveArtworkFile(context, artworkFilename, DIRECTORY_ARTIST_IMAGES, image);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        final ContentValues values = new ContentValues();
        values.put(ArtistArtTable.COLUMN_ARTIST_ID, artistIDString);
        values.put(ArtistArtTable.COLUMN_ARTIST_MBID, artistMBID);
        values.put(ArtistArtTable.COLUMN_ARTIST_NAME, artistName);
        values.put(ArtistArtTable.COLUMN_IMAGE_FILE_PATH, artworkFilename);

        // If null was given as byte[] set the not_found flag for this entry.
        values.put(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND, image == null ? 1 : 0);

        database.replace(ArtistArtTable.TABLE_NAME, "", values);

        database.close();
    }

    /**
     * Saves the given album byte[] image.
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

        String artworkFilename = null;
        if (image != null) {
            try {
                artworkFilename = FileUtils.createSHA256HashForString(albumID, albumMBID, albumName, albumArtistName) + ".jpg";
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return;
            }

            try {
                FileUtils.saveArtworkFile(context, artworkFilename, DIRECTORY_ALBUM_IMAGES, image);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        final ContentValues values = new ContentValues();
        values.put(AlbumArtTable.COLUMN_ALBUM_ID, albumID);
        values.put(AlbumArtTable.COLUMN_ALBUM_MBID, albumMBID);
        values.put(AlbumArtTable.COLUMN_ALBUM_NAME, albumName);
        values.put(AlbumArtTable.COLUMN_ARTIST_NAME, albumArtistName);
        values.put(AlbumArtTable.COLUMN_IMAGE_FILE_PATH, artworkFilename);

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

        FileUtils.removeArtworkDirectory(context, DIRECTORY_ARTIST_IMAGES);
    }

    /**
     * Removes all lines from the albums table
     */
    public synchronized void clearAlbumImages(final Context context) {
        final SQLiteDatabase database = getWritableDatabase();

        database.delete(AlbumArtTable.TABLE_NAME, null, null);

        database.close();

        FileUtils.removeArtworkDirectory(context, DIRECTORY_ALBUM_IMAGES);
    }

    /**
     * Reset the state of all artist images that was not found before
     */
    public synchronized void clearBlockedArtistImages() {
        final SQLiteDatabase database = getWritableDatabase();

        final String where = ArtistArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        final String whereArgs[] = {"1"};

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    /**
     * Reset the state of all album images that was not found before
     */
    public synchronized void clearBlockedAlbumImages() {
        final SQLiteDatabase database = getWritableDatabase();

        final String where = AlbumArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        final String whereArgs[] = {"1"};

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    /**
     * Removes the artist image for the given artist.
     *
     * @param context The application context to access the file.
     * @param artist  The {@link ArtistModel} representing the artist.
     */
    public synchronized void removeArtistImage(final Context context, final ArtistModel artist) {
        final SQLiteDatabase database = getWritableDatabase();

        final String where = ArtistArtTable.COLUMN_ARTIST_ID + "=? OR " + ArtistArtTable.COLUMN_ARTIST_NAME + "=?";
        final String whereArgs[] = {String.valueOf(artist.getArtistID()), artist.getArtistName()};

        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_IMAGE_FILE_PATH},
                where, whereArgs, null, null, null);

        if (requestCursor.moveToFirst()) {

            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();

            FileUtils.removeArtworkFile(context, artworkFilename, DIRECTORY_ARTIST_IMAGES);
        }

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    /**
     * Removes the album image for the given album.
     *
     * @param context The application context to access the file.
     * @param album   The {@link AlbumModel} representing the album.
     */
    public synchronized void removeAlbumImage(final Context context, final AlbumModel album) {
        final SQLiteDatabase database = getWritableDatabase();

        final long albumId = album.getAlbumID();
        final String albumName = album.getAlbumName();
        final String artistName = album.getArtistName();

        String where;
        String whereArgs[];

        if (albumId != -1) {
            where = AlbumArtTable.COLUMN_ALBUM_ID + "=?";
            whereArgs = new String[]{String.valueOf(albumId)};
        } else if (!artistName.isEmpty()) {
            where = AlbumArtTable.COLUMN_ALBUM_NAME + "=? AND " + AlbumArtTable.COLUMN_ARTIST_NAME + "=?";
            whereArgs = new String[]{albumName, artistName};
        } else {
            where = AlbumArtTable.COLUMN_ALBUM_NAME + "=?";
            whereArgs = new String[]{albumName};
        }

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_IMAGE_FILE_PATH},
                where, whereArgs, null, null, null);

        if (requestCursor.moveToFirst()) {

            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();

            FileUtils.removeArtworkFile(context, artworkFilename, DIRECTORY_ALBUM_IMAGES);
        }

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }
}
