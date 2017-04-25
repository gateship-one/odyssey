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

package org.gateshipone.odyssey.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATE_ADDED};

    public static final String[] projectionPlaylistTracks = {MediaStore.Audio.Playlists.Members.TITLE, MediaStore.Audio.Playlists.Members.DISPLAY_NAME, MediaStore.Audio.Playlists.Members.TRACK, MediaStore.Audio.Playlists.Members.ALBUM_KEY,
            MediaStore.Audio.Playlists.Members.DURATION, MediaStore.Audio.Playlists.Members.ALBUM, MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.DATA, MediaStore.Audio.Playlists.Members._ID, MediaStore.Audio.Playlists.Members.AUDIO_ID};

    public static final String[] projectionPlaylists = {MediaStore.Audio.Playlists.NAME, MediaStore.Audio.Playlists._ID};

    /**
     * Threshold how many items should be inserted in the mediastore at once.
     * The threshold is needed to not exceed the size of the binder IPC transaction buffer.
     */
    private static final int chunkSize = 1000;

    /**
     * Workaround to insert images for albums that are not part of the system media library and
     * therefore do not have an album id. The offset needs to be bigger then the count of
     * available albums in the system library.
     */
    private static final long ALBUMID_HASH_OFFSET = 5000000;

    /**
     * Return the artistId for the given artistname
     */
    public static long getArtistIDFromName(String artistName, Context context) {
        // get artist id
        long artistID = -1;

        String whereVal[] = {artistName};

        String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

        String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, projectionArtists, where, whereVal, orderBy);

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                artistID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            }

            cursor.close();
        }

        return artistID;
    }

    /**
     * Return an album model created by the given album key.
     *
     * @param albumKey The key to identify the album in the mediastore.
     * @param context  The current context.
     * @return The created album model.
     */
    public static AlbumModel createAlbumModelFromKey(final String albumKey, final Context context) {
        String whereVal[] = {albumKey};

        String where = MediaStore.Audio.Albums.ALBUM_KEY + "=?";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projectionAlbums, where, whereVal, null);

        AlbumModel albumModel = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {

                String albumTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                String albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                String artistTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                long albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));

                albumModel = new AlbumModel(albumTitle, albumArt, artistTitle, albumKey, albumID);
            }

            cursor.close();
        }

        return albumModel;
    }

    /**
     * Retrieves the album ID for the given album key
     *
     * @param albumKey The key to identify the album in the mediastore
     * @param context  Context used for the request
     * @return albumID if found or -1 if not found.
     */
    public static long getAlbumIDFromKey(String albumKey, Context context) {
        String whereVal[] = {albumKey};

        String where = MediaStore.Audio.Albums.ALBUM_KEY + "=?";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projectionAlbums, where, whereVal, null);

        long albumID = -1;

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
            }

            cursor.close();
        }

        // no album id found -> album not in mediastore; generate fake id
        if (albumID == -1) {
            albumID = albumKey.hashCode() + ALBUMID_HASH_OFFSET;
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

                    // add current track
                    albumTracks.add(new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id));

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
     * @param orderKey String to specify the order of the tracks
     */
    public static List<TrackModel> getTracksForArtist(long artistId, String orderKey, Context context) {
        List<TrackModel> artistTracks = new ArrayList<>();

        String orderBy;

        if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_name_key))) {
            orderBy = MediaStore.Audio.Albums.ALBUM;
        } else if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_year_key))) {
            orderBy = MediaStore.Audio.Albums.FIRST_YEAR;
        } else {
            orderBy = MediaStore.Audio.Albums.ALBUM;
        }

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.Albums.getContentUri("external", artistId),
                new String[]{MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Albums.FIRST_YEAR}, "", null, orderBy + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
                    artistTracks.addAll(getTracksForAlbum(albumKey, context));
                } while (cursor.moveToNext());
            }

            cursor.close();
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

                    // add the track
                    playlistTracks.add(new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return playlistTracks;
    }

    public static List<AlbumModel> getRecentAlbums(Context context) {
        List<AlbumModel> recentAlbums = new ArrayList<>();

        // get recent tracks
        // filter non music and tracks older than 4 weeks
        long fourWeeksAgo = (System.currentTimeMillis() / 1000) - (4 * 3600 * 24 * 7);

        String whereVal[] = {"1", String.valueOf(fourWeeksAgo)};

        String where = MediaStore.Audio.Media.IS_MUSIC + "=? AND " + MediaStore.Audio.Media.DATE_ADDED + ">?" + ") GROUP BY (" + MediaStore.Audio.Media.ALBUM_KEY;

        Cursor recentTracksCursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Media.DATE_ADDED}, where, whereVal, MediaStore.Audio.Media.ALBUM_KEY);

        // get all albums
        Cursor albumsCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM_KEY);

        if (recentTracksCursor != null && albumsCursor != null) {
            if (recentTracksCursor.moveToFirst() && albumsCursor.moveToFirst()) {

                int albumKeyColumnIndex = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
                int albumTitleColumnIndex = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int imagePathColumnIndex = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                int artistTitleColumnIndex = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
                int albumIDColumnIndex = albumsCursor.getColumnIndex(MediaStore.Audio.Albums._ID);

                int recentTracksAlbumKeyColumnIndex = recentTracksCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);
                int recentTracksDateAddedColumnIndex = recentTracksCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);

                do {
                    if (albumsCursor.getString(albumKeyColumnIndex).equals(recentTracksCursor.getString(recentTracksAlbumKeyColumnIndex))) {
                        String albumKey = albumsCursor.getString(albumKeyColumnIndex);
                        String albumTitle = albumsCursor.getString(albumTitleColumnIndex);
                        String imagePath = albumsCursor.getString(imagePathColumnIndex);
                        String artistTitle = albumsCursor.getString(artistTitleColumnIndex);
                        long albumID = albumsCursor.getLong(albumIDColumnIndex);

                        int dateInMillis = recentTracksCursor.getInt(recentTracksDateAddedColumnIndex);

                        // add the album
                        recentAlbums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumKey, albumID, dateInMillis));

                        if (!recentTracksCursor.moveToNext()) {
                            break;
                        }
                    }

                } while (albumsCursor.moveToNext());
            }
        }

        // sort the recent albums
        Collections.sort(recentAlbums, new Comparator<AlbumModel>() {
            @Override
            public int compare(AlbumModel o1, AlbumModel o2) {
                // sort by date descending
                if (o1.getDateAdded() < o2.getDateAdded()) {
                    return 1;
                } else if (o1.getDateAdded() == o2.getDateAdded()) {
                    // if equal date sort by key
                    return o1.getAlbumKey().compareTo(o2.getAlbumKey());
                } else {
                    return -1;
                }
            }
        });

        return recentAlbums;
    }

    public static List<TrackModel> getRecentTracks(Context context) {
        List<TrackModel> recentTracks = new ArrayList<>();

        // filter non music and tracks older than 4 weeks
        long fourWeeksAgo = (System.currentTimeMillis() / 1000) - (4 * 3600 * 24 * 7);

        String whereVal[] = {"1", String.valueOf(fourWeeksAgo)};

        String where = MediaStore.Audio.Media.IS_MUSIC + "=? AND " + MediaStore.Audio.Media.DATE_ADDED + ">?";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionTracks, where, whereVal, MediaStore.Audio.Media.ALBUM_KEY);

        if (cursor != null) {
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
                    int dateAdded = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));

                    // add the track
                    recentTracks.add(new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id, dateAdded));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        Collections.sort(recentTracks, new Comparator<TrackModel>() {
            @Override
            public int compare(TrackModel o1, TrackModel o2) {
                // sort tracks by albumkey
                if (o1.getTrackAlbumKey().equals(o2.getTrackAlbumKey())) {
                    // sort by tracknumber
                    if (o1.getTrackNumber() < o2.getTrackNumber()) {
                        return -1;
                    } else if (o1.getTrackNumber() == o2.getTrackNumber()) {
                        return 0;
                    } else {
                        return 1;
                    }
                } else {
                    // sort tracks by date descending
                    if (o1.getDateAdded() < o2.getDateAdded()) {
                        return 1;
                    } else if (o1.getDateAdded() == o2.getDateAdded()) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            }
        });


        return recentTracks;
    }

    /**
     * Return a list of all tracks in the mediastore.
     */
    public static List<TrackModel> getAllTracks(Context context) {
        List<TrackModel> allTracks = new ArrayList<>();

        // filter non music
        String whereVal[] = {"1"};

        String where = MediaStore.Audio.Media.IS_MUSIC + "=?";

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionTracks, where, whereVal, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");

        if (cursor != null) {
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

                    // add the track
                    allTracks.add(new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id));

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

    /**
     * Return a list of all albums in the mediastore.
     */
    public static List<AlbumModel> getAllAlbums(Context context) {
        ArrayList<AlbumModel> albums = new ArrayList<>();

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                int albumKeyColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
                int albumTitleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int imagePathColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                int artistTitleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
                int albumIDColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums._ID);

                do {
                    String albumKey = cursor.getString(albumKeyColumnIndex);
                    String albumTitle = cursor.getString(albumTitleColumnIndex);
                    String imagePath = cursor.getString(imagePathColumnIndex);
                    String artistTitle = cursor.getString(artistTitleColumnIndex);
                    long albumID = cursor.getLong(albumIDColumnIndex);

                    // add the album
                    albums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumKey, albumID));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }
        return albums;
    }

    /**
     * Return a list of all albums of an artist
     *
     * @param artistId The id to identify the artist in the mediastore
     * @param orderKey String to specify the order of the albums
     */
    public static List<AlbumModel> getAllAlbumsForArtist(long artistId, String orderKey, Context context) {
        ArrayList<AlbumModel> albums = new ArrayList<>();

        String orderBy;

        if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_name_key))) {
            orderBy = MediaStore.Audio.Albums.ALBUM;
        } else if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_year_key))) {
            orderBy = MediaStore.Audio.Albums.FIRST_YEAR;
        } else {
            orderBy = MediaStore.Audio.Albums.ALBUM;
        }

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.Albums.getContentUri("external", artistId), MusicLibraryHelper.projectionAlbums, "", null, orderBy + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                int albumKeyColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
                int albumTitleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int imagePathColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                int artistTitleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
                int albumIDColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums._ID);

                do {
                    String albumKey = cursor.getString(albumKeyColumnIndex);
                    String albumTitle = cursor.getString(albumTitleColumnIndex);
                    String imagePath = cursor.getString(imagePathColumnIndex);
                    String artistTitle = cursor.getString(artistTitleColumnIndex);
                    long albumID = cursor.getLong(albumIDColumnIndex);

                    // add the album
                    albums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumKey, albumID));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }
        return albums;
    }

    /**
     * Return a list of all artists in the mediastore.
     *
     * @param showAlbumArtistsOnly flag if only albumartists should be loaded
     */
    public static List<ArtistModel> getAllArtists(boolean showAlbumArtistsOnly, Context context) {
        ArrayList<ArtistModel> artists = new ArrayList<>();

        if (!showAlbumArtistsOnly) {
            // load all artists

            // get all artists
            Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null,
                    MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE ASC");

            if (cursor != null) {

                if (cursor.moveToFirst()) {
                    int artistTitleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
                    int artistIDColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Artists._ID);

                    do {
                        String artist = cursor.getString(artistTitleColumnIndex);
                        long artistID = cursor.getLong(artistIDColumnIndex);

                        // add the artist
                        artists.add(new ArtistModel(artist, artistID));

                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        } else {
            // load only artist which has an album entry

            // get all album covers
            Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM},
                    MediaStore.Audio.Albums.ARTIST + "<>\"\" ) GROUP BY (" + MediaStore.Audio.Albums.ARTIST, null, MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC");

            if (cursor != null) {

                if (cursor.moveToFirst()) {

                    int albumArtistTitleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);

                    do {
                        String artist = cursor.getString(albumArtistTitleColumnIndex);

                        // add the artist
                        artists.add(new ArtistModel(artist, -1));

                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        }
        return artists;
    }

    /**
     * Return a list of all playlists in the mediastore.
     */
    public static List<PlaylistModel> getAllPlaylists(Context context) {
        ArrayList<PlaylistModel> playlists = new ArrayList<>();

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionPlaylists, "", null, MediaStore.Audio.Playlists.NAME);

        if (cursor != null) {

            if (cursor.moveToFirst()) {
                int playlistTitleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);
                int playlistIDColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID);

                do {
                    String playlistTitle = cursor.getString(playlistTitleColumnIndex);
                    long playlistID = cursor.getLong(playlistIDColumnIndex);

                    // add the playlist
                    playlists.add(new PlaylistModel(playlistTitle, playlistID));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return playlists;
    }
}
