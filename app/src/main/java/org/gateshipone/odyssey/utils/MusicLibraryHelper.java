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

package org.gateshipone.odyssey.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.util.ArrayList;
import java.util.List;

public class MusicLibraryHelper {
    private static final String TAG = "MusicLibraryHelper";

    /**
     * Selection arrays for the different tables in the mediastore.
     */
    public static final String[] projectionAlbums = {MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.FIRST_YEAR, MediaStore.Audio.Albums.LAST_YEAR};

    public static final String[] projectionArtists = {MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.NUMBER_OF_TRACKS, MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS};

    public static final String[] projectionTracks = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media._ID};

    public static final String[] projectionPlaylistTracks = {MediaStore.Audio.Playlists.Members.TITLE, MediaStore.Audio.Playlists.Members.DISPLAY_NAME, MediaStore.Audio.Playlists.Members.TRACK, MediaStore.Audio.Playlists.Members.ALBUM_KEY,
            MediaStore.Audio.Playlists.Members.DURATION, MediaStore.Audio.Playlists.Members.ALBUM, MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.DATA, MediaStore.Audio.Playlists.Members._ID, MediaStore.Audio.Playlists.Members.AUDIO_ID};

    public static final String[] projectionPlaylists = {MediaStore.Audio.Playlists.NAME, MediaStore.Audio.Playlists._ID};

    /**
     * Threshold how many items should be inserted in the mediastore at once.
     * The threshold is needed to not exceed the size of the binder IPC transaction buffer.
     */
    private static final int chunkSize = 1000;

    /**
     * Return the artistId for the given artistname
     */
    public static long getArtistIDFromName(String artistName, Context context) {
        // get artist id
        long artistID = -1;

        String whereVal[] = {artistName};

        String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

        String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE";

        Cursor artistCursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, projectionArtists, where, whereVal, orderBy);

        if (artistCursor != null) {
            if (artistCursor.moveToFirst()) {

                artistID = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            }

            artistCursor.close();
        }

        return artistID;
    }

    /**
     * Return a list of albuminformations for the given albumKey.
     */
    public static ArrayList<String> getAlbumInformationFromKey(String albumKey, Context context) {
        String whereVal[] = {albumKey};

        String where = MediaStore.Audio.Albums.ALBUM_KEY + "=?";

        Cursor albumCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projectionAlbums, where, whereVal, null);

        ArrayList<String> albumInformations = new ArrayList<>();

        if (albumCursor != null) {
            if (albumCursor.moveToFirst()) {

                String albumTitle = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                String albumArt = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                String artistTitle = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));

                albumInformations.add(albumTitle);
                albumInformations.add(albumArt);
                albumInformations.add(artistTitle);
            }

            albumCursor.close();
        }

        return albumInformations;
    }

    /**
     * Retrieves the album ID for the given album key
     *
     * @param albumKey Key to use for retrieval
     * @param context  Context used for the request
     * @return albumID if found or -1 if not found.
     */
    public static long getAlbumIDFromKey(String albumKey, Context context) {
        String whereVal[] = {albumKey};

        String where = MediaStore.Audio.Albums.ALBUM_KEY + "=?";

        Cursor albumCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projectionAlbums, where, whereVal, null);

        long albumID = -1;

        if (albumCursor != null) {
            if (albumCursor.moveToFirst()) {

                albumID = albumCursor.getLong(albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID));
            }

            albumCursor.close();
        }

        return albumID;
    }

    /**
     * Return a list of all tracks of an album.
     *
     * @param albumKey The key to identify the album in the mediastore
     */
    public static List<TrackModel> getTracksForAlbum(String albumKey, Context context) {
        List<TrackModel> albumTracks = new ArrayList<>();

        String whereVal[] = {albumKey};

        String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

        String orderBy = android.provider.MediaStore.Audio.Media.TRACK;

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionTracks, where, whereVal, orderBy);

        if (cursor != null) {
            // get all tracks on the current album
            if (cursor.moveToFirst()) {
                do {
                    String trackName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    int number = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                    String artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                    TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id);

                    // add current track
                    albumTracks.add(item);

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return albumTracks;
    }

    /**
     * Return a list of all tracks of an artist
     *
     * @param artistId The id to identify the artist in the mediastore
     */
    public static List<TrackModel> getTracksForArtist(long artistId, Context context) {
        List<TrackModel> artistTracks = new ArrayList<>();

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String orderPref = sharedPref.getString("pref_album_sort_order", "name");

        String orderBy;

        switch (orderPref) {
            case "name":
                orderBy = MediaStore.Audio.Albums.ALBUM;
                break;
            case "year":
                orderBy = MediaStore.Audio.Albums.FIRST_YEAR;
                break;
            default:
                orderBy = MediaStore.Audio.Albums.ALBUM;
        }

        Cursor albumCursor = PermissionHelper.query(context, MediaStore.Audio.Artists.Albums.getContentUri("external", artistId),
                new String[]{MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Albums.FIRST_YEAR}, "", null, orderBy + " COLLATE NOCASE");

        if (albumCursor != null) {
            if (albumCursor.moveToFirst()) {
                do {
                    String albumKey = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
                    artistTracks.addAll(getTracksForAlbum(albumKey, context));
                } while (albumCursor.moveToNext());
            }
        }

        return artistTracks;
    }

    /**
     * Return a list of all tracks of a playlist
     *
     * @param playlistId The id to identify the playlist in the mediastore
     */
    public static List<TrackModel> getTracksForPlaylist(long playlistId, Context context) {
        List<TrackModel> playlistTracks = new ArrayList<>();

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId), projectionPlaylistTracks, "", null, "");

        if (cursor != null) {
            // get all tracks of the playlist
            if (cursor.moveToFirst()) {
                do {
                    String trackName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION));
                    int number = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
                    String artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST));
                    String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA));
                    String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_KEY));
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));

                    TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id);

                    // add the track
                    playlistTracks.add(item);

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return playlistTracks;
    }

    /**
     * Return a list of all tracks in the mediastore.
     */
    public static List<TrackModel> getAllTracks(Context context) {
        List<TrackModel> allTracks = new ArrayList<>();

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionTracks, "", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");

        if (cursor != null) {
            // add all tracks to playlist
            if (cursor.moveToFirst()) {

                do {
                    String trackName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    int number = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                    String artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                    TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id);

                    // add the track
                    allTracks.add(item);

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return allTracks;
    }

    /**
     * Save a playlist in the mediastore.
     * A previous playlist with the same name will be deleted.
     * Only tracks that exists in the mediastore will be saved in the playlist.
     *
     * @param playlistName The name for the playlist
     * @param tracks       The tracklist for the playlist
     */
    public static void savePlaylist(String playlistName, List<TrackModel> tracks, Context context) {
        // remove playlist if exists
        PermissionHelper.delete(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MediaStore.Audio.Playlists.NAME + "=?", new String[]{playlistName});

        // create new playlist and save row
        ContentValues inserts = new ContentValues();
        inserts.put(MediaStore.Audio.Playlists.NAME, playlistName);
        inserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        inserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        Uri currentRow = PermissionHelper.insert(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, inserts);

        // create list of valid tracks
        List<ContentValues> values = new ArrayList<>();

        if (currentRow != null) {

            for (int i = 0; i < tracks.size(); i++) {

                TrackModel item = tracks.get(i);

                if (item != null) {
                    long id = item.getTrackId();

                    if (id != -1) {
                        // only tracks that exists in the mediastore should be saved in the playlist

                        ContentValues insert = new ContentValues();
                        insert.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id);
                        insert.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);

                        values.add(insert);
                    }
                }

                if (values.size() > chunkSize) {
                    // insert valid tracks
                    PermissionHelper.bulkInsert(context, currentRow, values.toArray(new ContentValues[values.size()]));

                    values.clear();
                }
            }

            // insert valid tracks
            PermissionHelper.bulkInsert(context, currentRow, values.toArray(new ContentValues[values.size()]));
        }
    }

    public static List<AlbumModel> getAllAlbums(Context context) {
        // Create cursor for content retrieval
        Cursor albumCursor;

        // load all albums
        albumCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");

        ArrayList<AlbumModel> albums = new ArrayList<>();

        if (albumCursor != null) {
            if (albumCursor.moveToFirst()) {

                int albumKeyColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
                int albumTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int imagePathColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                int artistTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
                int albumIDColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID);

                do {
                    String albumKey = albumCursor.getString(albumKeyColumnIndex);
                    String albumTitle = albumCursor.getString(albumTitleColumnIndex);
                    String imagePath = albumCursor.getString(imagePathColumnIndex);
                    String artistTitle = albumCursor.getString(artistTitleColumnIndex);
                    long albumID = albumCursor.getLong(albumIDColumnIndex);
                    AlbumModel album = new AlbumModel(albumTitle, imagePath, artistTitle, albumKey, albumID);
                    albums.add(album);

                } while (albumCursor.moveToNext());
            }

            albumCursor.close();
        }
        return albums;
    }

    public static List<ArtistModel> getAllArtists(Context context) {
        // FIXME Remove the coverPath workaround
        ArrayList<ArtistModel> artists = new ArrayList<>();
        String artist, coverPath;

        SharedPreferences sharedPref = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(context);
        boolean showAlbumArtistsOnly = sharedPref.getBoolean("pref_key_album_artists_only", true);

        if (!showAlbumArtistsOnly) {
            // load all artists

            // get all artists
            Cursor artistCursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE ASC");

            if (artistCursor != null) {

                if (artistCursor.moveToFirst()) {

                    long artistID;

                    int artistTitleColumnIndex = artistCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
                    int artistIDColumnIndex = artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID);

                    do {
                        artist = artistCursor.getString(artistTitleColumnIndex);
                        artistID = artistCursor.getLong(artistIDColumnIndex);

                        artists.add(new ArtistModel(artist, null, artistID));

                    } while (artistCursor.moveToNext());
                }

                artistCursor.close();
            }
        } else {
            // load only artist which has an album entry

            // get all album covers
            Cursor albumArtCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM}, MediaStore.Audio.Albums.ARTIST + "<>\"\" ) GROUP BY (" + MediaStore.Audio.Albums.ARTIST, null,
                    MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC");

            if (albumArtCursor != null) {

                if (albumArtCursor.moveToFirst()) {

                    int albumArtistTitleColumnIndex = albumArtCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);

                    do {
                        artist = albumArtCursor.getString(albumArtistTitleColumnIndex);

                        artists.add(new ArtistModel(artist, null, -1));

                    } while (albumArtCursor.moveToNext());
                }

                albumArtCursor.close();
            }
        }
        return artists;
    }
}
