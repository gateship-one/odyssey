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

package org.gateshipone.odyssey.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.MediaStoreProjections.ProjectionAlbums;
import org.gateshipone.odyssey.utils.MediaStoreProjections.ProjectionArtists;
import org.gateshipone.odyssey.utils.MediaStoreProjections.ProjectionPlaylistTracks;
import org.gateshipone.odyssey.utils.MediaStoreProjections.ProjectionPlaylists;
import org.gateshipone.odyssey.utils.MediaStoreProjections.ProjectionTracks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MusicLibraryHelper {
    private static final String TAG = "MusicLibraryHelper";

    /**
     * Date limit used for recent album list generation (4 weeks)
     */
    private static final int recentDateLimit = (4 * 7 * 24 * 3600);

    /**
     * Threshold how many items should be inserted in the MediaStore at once.
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
     *
     * @param artistName The name of the artist.
     * @param context    The application context to access the content resolver.
     * @return artistId if found or -1 if not found.
     */
    public static long getArtistIDFromName(final String artistName, final Context context) {
        long artistID = -1;

        final String[] whereVal = {artistName};

        final String where = ProjectionArtists.ARTIST + "=?";

        final String orderBy = ProjectionArtists.ARTIST + " COLLATE NOCASE";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, ProjectionArtists.PROJECTION, where, whereVal, orderBy);

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                artistID = cursor.getLong(cursor.getColumnIndex(ProjectionArtists.ID));
            }

            cursor.close();
        }

        return artistID;
    }

    /**
     * Return an album model created by the given album key.
     *
     * @param albumKey The key to identify the album in the MediaStore.
     * @param context  The application context to access the content resolver.
     * @return The created {@link AlbumModel}
     */
    public static AlbumModel createAlbumModelFromKey(final String albumKey, final Context context) {
        final String[] whereVal = {albumKey};

        final String where = ProjectionAlbums.ALBUM_KEY + "=?";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, where, whereVal, null);

        AlbumModel albumModel = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {

                final String albumTitle = cursor.getString(cursor.getColumnIndex(ProjectionAlbums.ALBUM));
                final String albumArt = cursor.getString(cursor.getColumnIndex(ProjectionAlbums.ALBUM_ART));
                final String artistTitle = cursor.getString(cursor.getColumnIndex(ProjectionAlbums.ARTIST));
                final long albumID = cursor.getLong(cursor.getColumnIndex(ProjectionAlbums.ID));

                albumModel = new AlbumModel(albumTitle, albumArt, artistTitle, albumKey, albumID);
            }

            cursor.close();
        }

        return albumModel;
    }

    /**
     * Retrieves the album ID for the given album key
     *
     * @param albumKey The key to identify the album in the MediaStore
     * @param context  The application context to access the content resolver.
     * @return albumID if found or derived id based on the albumKey hash code
     */
    public static long getAlbumIDFromKey(final String albumKey, final Context context) {
        final String[] whereVal = {albumKey};

        final String where = ProjectionAlbums.ALBUM_KEY + "=?";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, where, whereVal, null);

        long albumID = -1;

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                albumID = cursor.getLong(cursor.getColumnIndex(ProjectionAlbums.ID));
            }

            cursor.close();
        }

        // no album id found -> album not in MediaStore; generate fake id
        if (albumID == -1) {
            albumID = albumKey.hashCode() + ALBUMID_HASH_OFFSET;
        }

        return albumID;
    }

    /**
     * Return a list of all tracks of an album.
     *
     * @param context  The application context to access the content resolver.
     * @param albumKey The key to identify the album in the MediaStore
     * @return The list of {@link TrackModel} of all tracks for the given album.
     */
    public static List<TrackModel> getTracksForAlbum(final String albumKey, final Context context) {
        final List<TrackModel> albumTracks = new ArrayList<>();

        final String[] whereVal = {albumKey};

        final String where = ProjectionTracks.ALBUM_KEY + "=?";

        final String orderBy = ProjectionTracks.TRACK;

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, orderBy);

        if (cursor != null) {
            // get all tracks on the current album
            if (cursor.moveToFirst()) {
                do {
                    final String trackName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndex(ProjectionTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ARTIST));
                    final String albumName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ALBUM));
                    final String url = cursor.getString(cursor.getColumnIndex(ProjectionTracks.DATA));
                    final long id = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.ID));

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
     * @param context  The application context to access the content resolver.
     * @param artistId The id to identify the artist in the MediaStore
     * @param orderKey String to specify the order of the tracks
     * @return The list of {@link TrackModel} of all tracks for the given artist in the specified order.
     */
    public static List<TrackModel> getTracksForArtist(final long artistId, final String orderKey, final Context context) {
        List<TrackModel> artistTracks = new ArrayList<>();

        String orderBy;

        if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_name_key))) {
            orderBy = ProjectionAlbums.ALBUM;
        } else if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_year_key))) {
            orderBy = ProjectionAlbums.FIRST_YEAR;
        } else {
            orderBy = ProjectionAlbums.ALBUM;
        }

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.Albums.getContentUri("external", artistId),
                new String[]{ProjectionAlbums.ALBUM, ProjectionAlbums.ALBUM_KEY, ProjectionAlbums.FIRST_YEAR}, "", null, orderBy + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String albumKey = cursor.getString(cursor.getColumnIndex(ProjectionAlbums.ALBUM_KEY));
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
     * @param playlistId The id to identify the playlist in the MediaStore
     * @param context    The application context to access the content resolver.
     * @return The list of {@link TrackModel} of all tracks for the specified playlist.
     */
    public static List<TrackModel> getTracksForPlaylist(final long playlistId, final Context context) {
        final List<TrackModel> playlistTracks = new ArrayList<>();

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId), ProjectionPlaylistTracks.PROJECTION, "", null, "");

        if (cursor != null) {
            // get all tracks of the playlist
            if (cursor.moveToFirst()) {
                do {
                    final String trackName = cursor.getString(cursor.getColumnIndex(ProjectionPlaylistTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndex(ProjectionPlaylistTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndex(ProjectionPlaylistTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndex(ProjectionPlaylistTracks.ARTIST));
                    final String albumName = cursor.getString(cursor.getColumnIndex(ProjectionPlaylistTracks.ALBUM));
                    final String url = cursor.getString(cursor.getColumnIndex(ProjectionPlaylistTracks.DATA));
                    final String albumKey = cursor.getString(cursor.getColumnIndex(ProjectionPlaylistTracks.ALBUM_KEY));
                    final long id = cursor.getLong(cursor.getColumnIndex(ProjectionPlaylistTracks.AUDIO_ID));

                    // add the track
                    playlistTracks.add(new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return playlistTracks;
    }

    /**
     * Returns a list of albums added in the last 4 weeks.
     *
     * @param context The application context to access the content resolver.
     * @return The list of {@link AlbumModel} of all albums found in the MediaStore that were added in the last 4 weeks.
     */
    public static List<AlbumModel> getRecentAlbums(final Context context) {
        final List<AlbumModel> recentAlbums = new ArrayList<>();

        // get recent tracks
        // filter non music and tracks older than 4 weeks
        final long fourWeeksAgo = (System.currentTimeMillis() / 1000) - recentDateLimit;

        final String[] whereVal = {"1", String.valueOf(fourWeeksAgo)};

        final String where = ProjectionTracks.IS_MUSIC + "=? AND " + ProjectionTracks.DATE_ADDED + ">?";

        final Cursor recentTracksCursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{ProjectionTracks.ALBUM_KEY, ProjectionTracks.DATE_ADDED}, where, whereVal, ProjectionTracks.ALBUM_KEY);

        // get all albums
        final Cursor albumsCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, "", null, ProjectionAlbums.ALBUM_KEY);

        if (recentTracksCursor != null && albumsCursor != null) {
            if (recentTracksCursor.moveToFirst() && albumsCursor.moveToFirst()) {

                final int albumKeyColumnIndex = albumsCursor.getColumnIndex(ProjectionAlbums.ALBUM_KEY);
                final int albumTitleColumnIndex = albumsCursor.getColumnIndex(ProjectionAlbums.ALBUM);
                final int imagePathColumnIndex = albumsCursor.getColumnIndex(ProjectionAlbums.ALBUM_ART);
                final int artistTitleColumnIndex = albumsCursor.getColumnIndex(ProjectionAlbums.ARTIST);
                final int albumIDColumnIndex = albumsCursor.getColumnIndex(ProjectionAlbums.ID);
                final int numberOfSongsColumnIndex = albumsCursor.getColumnIndex(ProjectionAlbums.NUMER_OF_SONGS);

                final int recentTracksAlbumKeyColumnIndex = recentTracksCursor.getColumnIndex(ProjectionTracks.ALBUM_KEY);
                final int recentTracksDateAddedColumnIndex = recentTracksCursor.getColumnIndex(ProjectionTracks.DATE_ADDED);

                do {
                    if (albumsCursor.getString(albumKeyColumnIndex).equals(recentTracksCursor.getString(recentTracksAlbumKeyColumnIndex))) {
                        final String albumKey = albumsCursor.getString(albumKeyColumnIndex);
                        final String albumTitle = albumsCursor.getString(albumTitleColumnIndex);
                        final String imagePath = albumsCursor.getString(imagePathColumnIndex);
                        final String artistTitle = albumsCursor.getString(artistTitleColumnIndex);
                        final long albumID = albumsCursor.getLong(albumIDColumnIndex);
                        final int numberOfSongs = albumsCursor.getInt(numberOfSongsColumnIndex) > 0 ? albumsCursor.getInt(numberOfSongsColumnIndex) : 1;

                        final int dateInMillis = recentTracksCursor.getInt(recentTracksDateAddedColumnIndex);

                        // add the album
                        recentAlbums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumKey, albumID, dateInMillis));

                        // workaround because no group command is allowed in ContentResolver query
                        boolean isEnd;
                        do {
                            isEnd = !recentTracksCursor.move(numberOfSongs);

                            if (isEnd) {
                                break;
                            }
                        } while (albumKey.equals(recentTracksCursor.getString(recentTracksAlbumKeyColumnIndex)));

                        if (isEnd) {
                            break;
                        }
                    }

                } while (albumsCursor.moveToNext());
            }
        }

        // sort the recent albums
        Collections.sort(recentAlbums, (o1, o2) -> {
            // sort by date descending
            if (o1.getDateAdded() < o2.getDateAdded()) {
                return 1;
            } else if (o1.getDateAdded() == o2.getDateAdded()) {
                // if equal date sort by key
                return o1.getAlbumKey().compareTo(o2.getAlbumKey());
            } else {
                return -1;
            }
        });

        return recentAlbums;
    }

    /**
     * Generates a {@link Map} of recent added dates per AlbumKey to unify the dates per album
     *
     * @param context The application context to access the content resolver.
     * @return HashMap of dates per AlbumKey
     */
    private static Map<String, Integer> getRecentAlbumDates(final Context context) {
        final HashMap<String, Integer> recentDates = new HashMap<>();

        // get recent tracks
        // filter non music and tracks older than 4 weeks
        final long fourWeeksAgo = (System.currentTimeMillis() / 1000) - recentDateLimit;

        final String[] whereVal = {"1", String.valueOf(fourWeeksAgo)};

        final String where = ProjectionTracks.IS_MUSIC + "=? AND " + ProjectionTracks.DATE_ADDED + ">?";

        final Cursor recentTracksCursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{ProjectionTracks.ALBUM_KEY, ProjectionTracks.DATE_ADDED}, where, whereVal, ProjectionTracks.ALBUM_KEY);

        // get all albums
        final Cursor albumsCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, "", null, ProjectionAlbums.ALBUM_KEY);

        if (recentTracksCursor != null && albumsCursor != null) {
            if (recentTracksCursor.moveToFirst() && albumsCursor.moveToFirst()) {

                final int albumKeyColumnIndex = albumsCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
                final int numberOfSongsColumnIndex = albumsCursor.getColumnIndex(ProjectionAlbums.NUMER_OF_SONGS);

                final int recentTracksAlbumKeyColumnIndex = recentTracksCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);
                final int recentTracksDateAddedColumnIndex = recentTracksCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);

                do {
                    if (albumsCursor.getString(albumKeyColumnIndex).equals(recentTracksCursor.getString(recentTracksAlbumKeyColumnIndex))) {
                        final String albumKey = albumsCursor.getString(albumKeyColumnIndex);
                        final int numberOfSongs = albumsCursor.getInt(numberOfSongsColumnIndex) > 0 ? albumsCursor.getInt(numberOfSongsColumnIndex) : 1;

                        final int dateInMillis = recentTracksCursor.getInt(recentTracksDateAddedColumnIndex);

                        // add the album
                        recentDates.put(albumKey, dateInMillis);

                        // workaround because no group command is allowed in ContentResolver query
                        boolean isEnd;
                        do {
                            isEnd = !recentTracksCursor.move(numberOfSongs);

                            if (isEnd) {
                                break;
                            }
                        } while (albumKey.equals(recentTracksCursor.getString(recentTracksAlbumKeyColumnIndex)));

                        if (isEnd) {
                            break;
                        }
                    }

                } while (albumsCursor.moveToNext());
            }
        }

        return recentDates;
    }

    /**
     * Return a list of all tracks add in the last 4 weeks.
     *
     * @param context The application context to access the content resolver.
     * @return The list of {@link TrackModel} of all tracks found in the MediaStore that were added in the last 4 weeks.
     */
    public static List<TrackModel> getRecentTracks(final Context context) {
        final List<TrackModel> recentTracks = new ArrayList<>();

        // Map to unify the date of all album tracks for distinct sort order
        final Map<String, Integer> albumDateMap = getRecentAlbumDates(context);

        // filter non music and tracks older than 4 weeks
        final long fourWeeksAgo = (System.currentTimeMillis() / 1000) - recentDateLimit;

        final String[] whereVal = {"1", String.valueOf(fourWeeksAgo)};

        final String where = ProjectionTracks.IS_MUSIC + "=? AND " + ProjectionTracks.DATE_ADDED + ">?";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, ProjectionTracks.ALBUM_KEY);

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                do {
                    final String trackName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndex(ProjectionTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ARTIST));
                    final String albumName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ALBUM));
                    final String url = cursor.getString(cursor.getColumnIndex(ProjectionTracks.DATA));
                    final String albumKey = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ALBUM_KEY));
                    final long id = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.ID));
                    final int dateAdded = albumDateMap.containsKey(albumKey) ? albumDateMap.get(albumKey) : -1;

                    // add the track
                    recentTracks.add(new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id, dateAdded));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        Collections.sort(recentTracks, (o1, o2) -> {
            // sort tracks by albumkey
            if (o1.getTrackAlbumKey().equals(o2.getTrackAlbumKey())) {
                // sort by tracknumber
                return Integer.compare(o1.getTrackNumber(), o2.getTrackNumber());
            } else {
                // sort tracks by date descending
                return Integer.compare(o2.getDateAdded(), o1.getDateAdded());
            }
        });

        return recentTracks;
    }

    /**
     * Return a list of all tracks in the MediaStore.
     *
     * @param filterString A filter that is used to exclude tracks that didn't contain this String.
     * @param context      The application context to access the content resolver.
     * @return The list of {@link TrackModel} of all tracks found in the MediaStore that matches the filter criteria.
     */
    public static List<TrackModel> getAllTracks(final String filterString, final Context context) {
        final List<TrackModel> allTracks = new ArrayList<>();

        // filter non music
        final String[] whereVal = {"1"};

        final String where = ProjectionTracks.IS_MUSIC + "=?";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, ProjectionTracks.TITLE + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                do {
                    final String trackName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndex(ProjectionTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ARTIST));
                    final String albumName = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ALBUM));
                    final String url = cursor.getString(cursor.getColumnIndex(ProjectionTracks.DATA));
                    final String albumKey = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ALBUM_KEY));
                    final long id = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.ID));

                    // add the track
                    if (null == filterString || filterString.isEmpty() || trackName.toLowerCase().contains(filterString)) {
                        allTracks.add(new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url, id));
                    }

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return allTracks;
    }

    /**
     * Save a playlist in the MediaStore.
     * A previous playlist with the same name will be deleted.
     * Only tracks that exists in the MediaStore will be saved in the playlist.
     *
     * @param playlistName The name for the playlist
     * @param tracks       The tracklist for the playlist
     * @param context      The application context to access the content resolver.
     */
    public static void savePlaylist(final String playlistName, final List<TrackModel> tracks, final Context context) {
        // remove playlist if exists
        PermissionHelper.delete(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, ProjectionPlaylists.NAME + "=?", new String[]{playlistName});

        // create new playlist and save row
        final ContentValues inserts = new ContentValues();
        inserts.put(ProjectionPlaylists.NAME, playlistName);
        inserts.put(ProjectionPlaylists.DATE_ADDED, System.currentTimeMillis());
        inserts.put(ProjectionPlaylists.DATE_MODIFIED, System.currentTimeMillis());

        final Uri currentRow = PermissionHelper.insert(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, inserts);

        // create list of valid tracks
        final List<ContentValues> values = new ArrayList<>();

        if (currentRow != null) {

            for (int i = 0; i < tracks.size(); i++) {

                final TrackModel item = tracks.get(i);

                if (item != null) {
                    final long id = item.getTrackId();

                    if (id != -1) {
                        // only tracks that exists in the MediaStore should be saved in the playlist

                        final ContentValues insert = new ContentValues();
                        insert.put(ProjectionPlaylistTracks.AUDIO_ID, id);
                        insert.put(ProjectionPlaylistTracks.PLAY_ORDER, i);

                        values.add(insert);
                    }
                }

                if (values.size() > chunkSize) {
                    // insert valid tracks
                    PermissionHelper.bulkInsert(context, currentRow, values.toArray(new ContentValues[0]));

                    values.clear();
                }
            }

            // insert valid tracks
            PermissionHelper.bulkInsert(context, currentRow, values.toArray(new ContentValues[0]));
        }
    }

    /**
     * Return a list of all albums in the MediaStore.
     *
     * @param context The application context to access the content resolver.
     * @return The list of {@link AlbumModel} of all albums found in the MediaStore.
     */
    public static List<AlbumModel> getAllAlbums(final Context context) {
        final ArrayList<AlbumModel> albums = new ArrayList<>();

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, "", null, ProjectionAlbums.ALBUM + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                final int albumKeyColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ALBUM_KEY);
                final int albumTitleColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ALBUM);
                final int imagePathColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ALBUM_ART);
                final int artistTitleColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ARTIST);
                final int albumIDColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ID);

                do {
                    final String albumKey = cursor.getString(albumKeyColumnIndex);
                    final String albumTitle = cursor.getString(albumTitleColumnIndex);
                    final String imagePath = cursor.getString(imagePathColumnIndex);
                    final String artistTitle = cursor.getString(artistTitleColumnIndex);
                    final long albumID = cursor.getLong(albumIDColumnIndex);

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
     * @param artistId The id to identify the artist in the MediaStore
     * @param orderKey String to specify the order of the albums
     * @param context  The application context to access the content resolver.
     * @return The list of {@link AlbumModel} of all albums of the artists in the specified order.
     */
    public static List<AlbumModel> getAllAlbumsForArtist(final long artistId, final String orderKey, final Context context) {
        final ArrayList<AlbumModel> albums = new ArrayList<>();

        String orderBy;

        if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_name_key))) {
            orderBy = ProjectionAlbums.ALBUM;
        } else if (orderKey.equals(context.getString(R.string.pref_artist_albums_sort_year_key))) {
            orderBy = ProjectionAlbums.FIRST_YEAR;
        } else {
            orderBy = ProjectionAlbums.ALBUM;
        }

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.Albums.getContentUri("external", artistId), ProjectionAlbums.PROJECTION, "", null, orderBy + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                final int albumKeyColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ALBUM_KEY);
                final int albumTitleColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ALBUM);
                final int imagePathColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ALBUM_ART);
                final int artistTitleColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ARTIST);
                final int albumIDColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ID);

                do {
                    final String albumKey = cursor.getString(albumKeyColumnIndex);
                    final String albumTitle = cursor.getString(albumTitleColumnIndex);
                    final String imagePath = cursor.getString(imagePathColumnIndex);
                    final String artistTitle = cursor.getString(artistTitleColumnIndex);
                    final long albumID = cursor.getLong(albumIDColumnIndex);

                    // add the album
                    albums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumKey, albumID));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }
        return albums;
    }

    /**
     * Return a list of all artists in the MediaStore.
     *
     * @param showAlbumArtistsOnly flag if only albumartists should be loaded
     * @param context              The application context to access the content resolver.
     * @return The list of {@link ArtistModel} of all artists found in the MediaStore that matches the filter criteria.
     */
    public static List<ArtistModel> getAllArtists(final boolean showAlbumArtistsOnly, final Context context) {
        final ArrayList<ArtistModel> artists = new ArrayList<>();

        if (!showAlbumArtistsOnly) {
            // load all artists

            // get all artists
            final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, ProjectionArtists.PROJECTION,
                    "", null, ProjectionArtists.ARTIST + " COLLATE NOCASE ASC");

            if (cursor != null) {

                if (cursor.moveToFirst()) {
                    final int artistTitleColumnIndex = cursor.getColumnIndex(ProjectionArtists.ARTIST);
                    final int artistIDColumnIndex = cursor.getColumnIndex(ProjectionArtists.ID);

                    do {
                        final String artist = cursor.getString(artistTitleColumnIndex);
                        final long artistID = cursor.getLong(artistIDColumnIndex);

                        // add the artist
                        artists.add(new ArtistModel(artist, artistID));

                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        } else {
            // load only artist which has an album entry
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // get all albums and use ARTIST_ID to identify album artists
                final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION,
                        "", null, ProjectionAlbums.ARTIST + " COLLATE NOCASE ASC");

                if (cursor != null) {

                    if (cursor.moveToFirst()) {

                        final int albumArtistTitleColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ARTIST);
                        final int albumIDColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ARTIST_ID);

                        // workaround because no group command is allowed in ContentResolver query
                        long lastArtistID = -1;

                        do {
                            final String artist = cursor.getString(albumArtistTitleColumnIndex);
                            final long artistID = cursor.getLong(albumIDColumnIndex);

                            // add the artist
                            // workaround because no group command is allowed in ContentResolver query
                            if (lastArtistID == -1 || lastArtistID != artistID) {
                                artists.add(new ArtistModel(artist, artistID));
                                lastArtistID = artistID;
                            }

                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                }
            } else {
                // get all albums grouped by artist
                final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{ProjectionAlbums.ARTIST, ProjectionAlbums.ALBUM},
                        ProjectionAlbums.ARTIST + "<>\"\" ) GROUP BY (" + ProjectionAlbums.ARTIST, null, ProjectionAlbums.ARTIST + " COLLATE NOCASE ASC");

                if (cursor != null) {

                    if (cursor.moveToFirst()) {

                        int albumArtistTitleColumnIndex = cursor.getColumnIndex(ProjectionAlbums.ARTIST);

                        do {
                            String artist = cursor.getString(albumArtistTitleColumnIndex);

                            // add the artist
                            artists.add(new ArtistModel(artist, -1));

                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                }
            }
        }
        return artists;
    }

    /**
     * Return a list of all playlists in the MediaStore.
     *
     * @param context The application context to access the content resolver.
     * @return The list of {@link PlaylistModel} of all playlists found in the MediaStore.
     */
    public static List<PlaylistModel> getAllPlaylists(final Context context) {
        final ArrayList<PlaylistModel> playlists = new ArrayList<>();

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, ProjectionPlaylists.PROJECTION, "", null, ProjectionPlaylists.NAME);

        if (cursor != null) {

            if (cursor.moveToFirst()) {
                final int playlistTitleColumnIndex = cursor.getColumnIndex(ProjectionPlaylists.NAME);
                final int playlistIDColumnIndex = cursor.getColumnIndex(ProjectionPlaylists.ID);

                do {
                    final String playlistTitle = cursor.getString(playlistTitleColumnIndex);
                    final long playlistID = cursor.getLong(playlistIDColumnIndex);

                    // add the playlist
                    playlists.add(new PlaylistModel(playlistTitle, playlistID));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return playlists;
    }

    /**
     * Removes a playlist from the MediaStore.
     *
     * @param playlistId The id of the playlist that should be removed.
     * @param context    The application context to access the content resolver.
     * @return The result of the operation. True if the playlist was removed else false.
     */
    public static boolean removePlaylist(final long playlistId, final Context context) {
        final String where = ProjectionPlaylists.ID + "=?";
        final String[] whereVal = {"" + playlistId};

        final int removedRows = PermissionHelper.delete(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal);

        return removedRows > 0;
    }

    /**
     * Removes a track from a playlist from the MediaStore.
     *
     * @param playlistId    The id of the playlist that contains the track.
     * @param trackPosition The position of the track that should be removed inside the playlist.
     * @param context       The application context to access the content resolver.
     * @return The result of the operation. True if the track was removed else false.
     */
    public static boolean removeTrackFromPlaylist(final long playlistId, final int trackPosition, final Context context) {
        final Uri playlistContentUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);

        final Cursor trackCursor = PermissionHelper.query(context, playlistContentUri, ProjectionPlaylistTracks.PROJECTION, "", null, "");

        int removedRows = -1;

        if (trackCursor != null) {
            if (trackCursor.moveToPosition(trackPosition)) {
                final String where = ProjectionPlaylistTracks.ID + "=?";
                final String[] whereVal = {trackCursor.getString(trackCursor.getColumnIndex(ProjectionPlaylistTracks.ID))};

                removedRows = PermissionHelper.delete(context, playlistContentUri, where, whereVal);
            }

            trackCursor.close();
        }

        return removedRows > 0;
    }

    /**
     * This method returns the storage location for each track that is connected to the provided album key.
     *
     * @param albumKey The album key that will be used to get all tracks for this key.
     * @param context  The application context to access the content resolver.
     * @return A {@link Set} of all storage locations for each track for the given album key.
     */
    public static Set<String> getTrackStorageLocationsForAlbum(final String albumKey, final Context context) {
        final Set<String> trackStorageLocations = new HashSet<>();

        final String[] whereVal = {albumKey};

        final String where = ProjectionTracks.ALBUM_KEY + "=?";

        final String orderBy = ProjectionTracks.TRACK;

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, orderBy);

        if (cursor != null) {
            // get all tracks on the current album
            if (cursor.moveToFirst()) {
                do {
                    final String url = cursor.getString(cursor.getColumnIndex(ProjectionTracks.DATA));

                    final File file = new File(url);
                    if (file.exists()) {
                        final String folderPath = file.getParent();
                        if (folderPath != null) {
                            trackStorageLocations.add(folderPath);
                        }
                    }
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return trackStorageLocations;
    }


    /**
     * Create and returns a {@link TrackModel} from the given {@link Uri}.
     *
     * @param uri     The {@link Uri} of the track.
     * @param context The application context to access the content resolver.
     * @return The created {@link TrackModel} or null if the track couldn't be found in the MediaStore.
     */
    static TrackModel getTrackForUri(final Uri uri, final Context context) {
        final String uriPath = uri.getPath();
        final String uriScheme = uri.getScheme();
        final String uriLastPathSegment = uri.getLastPathSegment();

        if (uriPath == null) {
            return null;
        }

        TrackModel track = null;

        String[] whereVal = {uri.getPath()};

        String where = ProjectionTracks.DATA + "=?";

        if (uriScheme != null && uriScheme.equals("content")) {
            // special handling for content urls
            final String[] parts = uriLastPathSegment != null ? uriLastPathSegment.split(":") : null;

            if (parts != null && parts.length > 1) {
                if (parts[0].equals("audio")) {
                    whereVal = new String[]{parts[1]};
                    where = ProjectionTracks.ID + "=?";
                } else {
                    whereVal = new String[]{"%" + parts[1]};
                    where = ProjectionTracks.DATA + " LIKE ?";
                }
            }
        }

        // lookup the current file in the media db
        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, ProjectionTracks.TRACK);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String title = cursor.getString(cursor.getColumnIndex(ProjectionTracks.TITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.DURATION));
                int no = cursor.getInt(cursor.getColumnIndex(ProjectionTracks.TRACK));
                String artist = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ALBUM));
                String url = cursor.getString(cursor.getColumnIndex(ProjectionTracks.DATA));
                String albumKey = cursor.getString(cursor.getColumnIndex(ProjectionTracks.ALBUM_KEY));
                long id = cursor.getLong(cursor.getColumnIndex(ProjectionTracks.ID));

                track = new TrackModel(title, artist, album, albumKey, duration, no, url, id);
            }

            cursor.close();
        }

        return track;
    }

    /**
     * Create a list of {@link FileModel} that represents all music files found in the MediaStore for the given path.
     *
     * @param basePath The path that should be used for the request.
     * @param context  The application context to access the content resolver.
     * @return The list of {@link FileModel} of all music files.
     */
    static List<FileModel> getMediaFilesForPath(final String basePath, final Context context) {
        final List<FileModel> files = new ArrayList<>();

        final String[] whereVal = {basePath + "%"};

        final String where = ProjectionTracks.DATA + " LIKE ?";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, ProjectionTracks.TRACK);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    final String url = cursor.getString(cursor.getColumnIndex(ProjectionTracks.DATA));

                    files.add(new FileModel(url));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return files;
    }
}
