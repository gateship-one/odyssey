/*
 * Copyright (C) 2023 Team Gateship-One
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

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.Nullable;

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
     * Return the artistId for the given artistname
     *
     * @param artistName The name of the artist.
     * @param context    The application context to access the content resolver.
     * @return artistId if found or -1 if not found.
     */
    public static long getArtistIDFromName(final String artistName, final Context context) {
        long artistId = -1;

        final String[] whereVal = {artistName};

        final String where = ProjectionArtists.ARTIST + "=?";

        final String orderBy = ProjectionArtists.ARTIST + " COLLATE NOCASE";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, ProjectionArtists.PROJECTION, where, whereVal, orderBy);

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                artistId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionArtists.ID));
            }

            cursor.close();
        }

        return artistId;
    }

    /**
     * Return an album model created by the given album id.
     *
     * @param albumId The id to identify the album in the MediaStore.
     * @param context The application context to access the content resolver.
     * @return The created {@link AlbumModel}
     */
    @Nullable
    public static AlbumModel createAlbumModelFromId(final long albumId, final Context context) {
        final String[] whereVal = {String.valueOf(albumId)};

        final String where = ProjectionAlbums.ID + "=?";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, where, whereVal, null);

        AlbumModel albumModel = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {

                final String albumTitle = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM));
                final String albumArt = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM_ART));
                final String artistTitle = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionAlbums.ARTIST));

                albumModel = new AlbumModel(albumTitle, albumArt, artistTitle, albumId);
            }

            cursor.close();
        }

        return albumModel;
    }

    /**
     * Retrieves the album ID for the given album key
     *
     * @param albumId    The id to identify the album in the MediaStore.
     * @param albumName  The name of the album, used as a fall back mechanism to retrieve the album id.
     * @param artistName The name of the artist, used as a fall back mechanism to retrieve the album id.
     * @param context    The application context to access the content resolver.
     * @return albumId if found or -1
     */
    public static long verifyAlbumId(final long albumId, final String albumName, final String artistName, final Context context) {
        long retrievedAlbumId = -1;

        if (albumId != -1) {
            final String[] whereVal = {String.valueOf(albumId)};

            final String where = ProjectionAlbums.ID + "=?";

            final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, where, whereVal, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {

                    retrievedAlbumId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionAlbums.ID));
                }

                cursor.close();
            }
        } else if (!TextUtils.isEmpty(albumName) && !TextUtils.isEmpty(artistName)) {
            // if no valid album id is given try a fall back mechanism
            // use album name and artist name to retrieve the album id
            // this method might not return a unique result

            final String[] whereVal = {albumName, artistName};

            final String where = ProjectionAlbums.ALBUM + "=? AND " + ProjectionAlbums.ARTIST + "=?";

            final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, where, whereVal, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {

                    retrievedAlbumId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionAlbums.ID));
                }

                cursor.close();
            }
        }

        return retrievedAlbumId;
    }

    /**
     * Return a list of all tracks of an album.
     *
     * @param context  The application context to access the content resolver.
     * @param orderKey String to specify the order of the tracks
     * @param albumId  The id to identify the album in the MediaStore
     * @return The list of {@link TrackModel} of all tracks for the given album.
     */
    public static List<TrackModel> getTracksForAlbum(final long albumId, final String orderKey, final Context context) {
        final List<TrackModel> albumTracks = new ArrayList<>();

        final String[] whereVal = {String.valueOf(albumId), "1"};

        final String where = ProjectionTracks.ALBUM_ID + "=? AND " + ProjectionTracks.IS_MUSIC + "=?";

        String orderBy;

        if (orderKey.equals(context.getString(R.string.pref_album_tracks_sort_number_key))) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                orderBy = "CAST(" + ProjectionTracks.TRACK + " AS unsigned)";
            }
            else{
                orderBy = ProjectionTracks.TRACK;
            }
        } else if (orderKey.equals(context.getString(R.string.pref_album_tracks_sort_name_key))) {
            orderBy = ProjectionTracks.DISPLAY_NAME;
        } else {
            orderBy = ProjectionTracks.TRACK;
        }

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, orderBy);

        if (cursor != null) {
            // get all tracks on the current album
            if (cursor.moveToFirst()) {
                do {
                    final String trackName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndexOrThrow(ProjectionTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST));
                    final String albumName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM));
                    final long artistId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST_ID));
                    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ID));

                    final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    // add current track
                    albumTracks.add(new TrackModel(trackName, artistName, artistId, albumName, albumId, duration, number, uri, id));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return albumTracks;
    }

    /**
     * Return a list of all tracks of an artist
     *
     * @param context       The application context to access the content resolver.
     * @param artistId      The id to identify the artist in the MediaStore
     * @param albumOrderKey String to specify the order of the artist albums
     * @param trackOrderKey String to specify the order of the tracks
     * @return The list of {@link TrackModel} of all tracks for the given artist in the specified order.
     */
    public static List<TrackModel> getTracksForArtist(final long artistId, final String albumOrderKey, final String trackOrderKey, final Context context) {
        List<TrackModel> artistTracks = new ArrayList<>();

        String orderBy;

        if (albumOrderKey.equals(context.getString(R.string.pref_artist_albums_sort_name_key))) {
            orderBy = ProjectionAlbums.ALBUM;
        } else if (albumOrderKey.equals(context.getString(R.string.pref_artist_albums_sort_year_key))) {
            orderBy = ProjectionAlbums.FIRST_YEAR;
        } else {
            orderBy = ProjectionAlbums.ALBUM;
        }

        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Artists.Albums.getContentUri("external", artistId),
                new String[]{ProjectionAlbums.ALBUM, ProjectionAlbums.ID, ProjectionAlbums.FIRST_YEAR}, "", null, orderBy + " COLLATE NOCASE");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionAlbums.ID));
                    artistTracks.addAll(getTracksForAlbum(albumId, trackOrderKey, context));
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
     * @deprecated Starting with API Level 30 the support for playlists in the mediastore will end.
     */
    public static List<TrackModel> getTracksForPlaylist(final long playlistId, final Context context) {
        final List<TrackModel> playlistTracks = new ArrayList<>();

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId), ProjectionPlaylistTracks.PROJECTION, "", null, "");

        if (cursor != null) {
            // get all tracks of the playlist
            if (cursor.moveToFirst()) {
                do {
                    final String trackName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.ARTIST));
                    final long artistId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.ARTIST_ID));
                    final String albumName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.ALBUM));
                    final long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.ALBUM_ID));
                    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionPlaylistTracks.AUDIO_ID));

                    final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    // add the track
                    playlistTracks.add(new TrackModel(trackName, artistName, artistId, albumName, albumId, duration, number, uri, id));

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

        final Cursor recentTracksCursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{ProjectionTracks.ALBUM_ID, ProjectionTracks.DATE_ADDED}, where, whereVal, ProjectionTracks.ALBUM_ID);

        // get all albums
        final Cursor albumsCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, "", null, ProjectionAlbums.ID);

        if (recentTracksCursor != null && albumsCursor != null) {
            if (recentTracksCursor.moveToFirst() && albumsCursor.moveToFirst()) {

                final int albumIdColumnIndex = albumsCursor.getColumnIndexOrThrow(ProjectionAlbums.ID);
                final int albumTitleColumnIndex = albumsCursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM);
                final int imagePathColumnIndex = albumsCursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM_ART);
                final int artistTitleColumnIndex = albumsCursor.getColumnIndexOrThrow(ProjectionAlbums.ARTIST);
                final int numberOfSongsColumnIndex = albumsCursor.getColumnIndexOrThrow(ProjectionAlbums.NUMER_OF_SONGS);

                final int recentTracksAlbumIdColumnIndex = recentTracksCursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM_ID);
                final int recentTracksDateAddedColumnIndex = recentTracksCursor.getColumnIndexOrThrow(ProjectionTracks.DATE_ADDED);

                do {
                    if (albumsCursor.getLong(albumIdColumnIndex) == recentTracksCursor.getLong(recentTracksAlbumIdColumnIndex)) {
                        final long albumId = albumsCursor.getLong(albumIdColumnIndex);
                        final String albumTitle = albumsCursor.getString(albumTitleColumnIndex);
                        final String imagePath = albumsCursor.getString(imagePathColumnIndex);
                        final String artistTitle = albumsCursor.getString(artistTitleColumnIndex);
                        final int numberOfSongs = albumsCursor.getInt(numberOfSongsColumnIndex) > 0 ? albumsCursor.getInt(numberOfSongsColumnIndex) : 1;

                        final int dateInMillis = recentTracksCursor.getInt(recentTracksDateAddedColumnIndex);

                        // add the album
                        recentAlbums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumId, dateInMillis));

                        // workaround because no group command is allowed in ContentResolver query
                        boolean isEnd;
                        do {
                            isEnd = !recentTracksCursor.move(numberOfSongs);

                            if (isEnd) {
                                break;
                            }
                        } while (albumId == recentTracksCursor.getLong(recentTracksAlbumIdColumnIndex));

                        if (isEnd) {
                            break;
                        }
                    }

                } while (albumsCursor.moveToNext());
            }

            albumsCursor.close();
            recentTracksCursor.close();
        }

        // sort the recent albums
        Collections.sort(recentAlbums, (o1, o2) -> {
            // sort by date descending
            if (o1.getDateAdded() < o2.getDateAdded()) {
                return 1;
            } else if (o1.getDateAdded() == o2.getDateAdded()) {
                // if equal date sort by key
                return Long.compare(o1.getAlbumId(), o2.getAlbumId());
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
    private static Map<Long, Integer> getRecentAlbumDates(final Context context) {
        final HashMap<Long, Integer> recentDates = new HashMap<>();

        // get recent tracks
        // filter non music and tracks older than 4 weeks
        final long fourWeeksAgo = (System.currentTimeMillis() / 1000) - recentDateLimit;

        final String[] whereVal = {"1", String.valueOf(fourWeeksAgo)};

        final String where = ProjectionTracks.IS_MUSIC + "=? AND " + ProjectionTracks.DATE_ADDED + ">?";

        final Cursor recentTracksCursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{ProjectionTracks.ALBUM_ID, ProjectionTracks.DATE_ADDED}, where, whereVal, ProjectionTracks.ALBUM_ID);

        // get all albums
        final Cursor albumsCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ProjectionAlbums.PROJECTION, "", null, ProjectionAlbums.ID);

        if (recentTracksCursor != null && albumsCursor != null) {
            if (recentTracksCursor.moveToFirst() && albumsCursor.moveToFirst()) {

                final int albumIdColumnIndex = albumsCursor.getColumnIndexOrThrow(ProjectionAlbums.ID);
                final int numberOfSongsColumnIndex = albumsCursor.getColumnIndexOrThrow(ProjectionAlbums.NUMER_OF_SONGS);

                final int recentTracksAlbumIdColumnIndex = recentTracksCursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM_ID);
                final int recentTracksDateAddedColumnIndex = recentTracksCursor.getColumnIndexOrThrow(ProjectionTracks.DATE_ADDED);

                do {
                    if (albumsCursor.getLong(albumIdColumnIndex) == recentTracksCursor.getLong(recentTracksAlbumIdColumnIndex)) {
                        final long albumId = albumsCursor.getLong(albumIdColumnIndex);
                        final int numberOfSongs = albumsCursor.getInt(numberOfSongsColumnIndex) > 0 ? albumsCursor.getInt(numberOfSongsColumnIndex) : 1;

                        final int dateInMillis = recentTracksCursor.getInt(recentTracksDateAddedColumnIndex);

                        // add the album
                        recentDates.put(albumId, dateInMillis);

                        // workaround because no group command is allowed in ContentResolver query
                        boolean isEnd;
                        do {
                            isEnd = !recentTracksCursor.move(numberOfSongs);

                            if (isEnd) {
                                break;
                            }
                        } while (albumId == recentTracksCursor.getLong(recentTracksAlbumIdColumnIndex));

                        if (isEnd) {
                            break;
                        }
                    }

                } while (albumsCursor.moveToNext());
            }

            albumsCursor.close();
            recentTracksCursor.close();
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
        final Map<Long, Integer> albumDateMap = getRecentAlbumDates(context);

        // filter non music and tracks older than 4 weeks
        final long fourWeeksAgo = (System.currentTimeMillis() / 1000) - recentDateLimit;

        final String[] whereVal = {"1", String.valueOf(fourWeeksAgo)};

        final String where = ProjectionTracks.IS_MUSIC + "=? AND " + ProjectionTracks.DATE_ADDED + ">?";

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, ProjectionTracks.ALBUM_ID);

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                do {
                    final String trackName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndexOrThrow(ProjectionTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST));
                    final long artistId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST_ID));
                    final String albumName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM));
                    final long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM_ID));
                    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ID));
                    final int dateAdded = albumDateMap.containsKey(albumId) ? albumDateMap.get(albumId) : -1;

                    final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    // add the track
                    recentTracks.add(new TrackModel(trackName, artistName, artistId, albumName, albumId, duration, number, uri, id, dateAdded));

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        Collections.sort(recentTracks, (o1, o2) -> {
            // sort tracks by albumkey
            if (o1.getTrackAlbumId() == o2.getTrackAlbumId()) {
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
                    final String trackName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.TITLE));
                    final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.DURATION));
                    final int number = cursor.getInt(cursor.getColumnIndexOrThrow(ProjectionTracks.TRACK));
                    final String artistName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST));
                    final long artistId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST_ID));
                    final String albumName = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM));
                    final long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM_ID));
                    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ID));

                    final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    // add the track
                    if (null == filterString || filterString.isEmpty() || trackName.toLowerCase().contains(filterString)) {
                        allTracks.add(new TrackModel(trackName, artistName, artistId, albumName, albumId, duration, number, uri, id));
                    }

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return allTracks;
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
                final int albumTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM);
                final int imagePathColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM_ART);
                final int artistTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ARTIST);
                final int albumIdColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ID);

                do {
                    final String albumTitle = cursor.getString(albumTitleColumnIndex);
                    final String imagePath = cursor.getString(imagePathColumnIndex);
                    final String artistTitle = cursor.getString(artistTitleColumnIndex);
                    final long albumId = cursor.getLong(albumIdColumnIndex);

                    // add the album
                    albums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumId));

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
                final int albumTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM);
                final int imagePathColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ALBUM_ART);
                final int artistTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ARTIST);
                final int albumIdColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ID);

                do {
                    final String albumTitle = cursor.getString(albumTitleColumnIndex);
                    final String imagePath = cursor.getString(imagePathColumnIndex);
                    final String artistTitle = cursor.getString(artistTitleColumnIndex);
                    final long albumId = cursor.getLong(albumIdColumnIndex);

                    // add the album
                    albums.add(new AlbumModel(albumTitle, imagePath, artistTitle, albumId));
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
                    final int artistTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionArtists.ARTIST);
                    final int artistIdColumnIndex = cursor.getColumnIndexOrThrow(ProjectionArtists.ID);

                    do {
                        final String artist = cursor.getString(artistTitleColumnIndex);
                        final long artistId = cursor.getLong(artistIdColumnIndex);

                        // add the artist
                        artists.add(new ArtistModel(artist, artistId));

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

                        final int albumArtistTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ARTIST);
                        final int albumIdColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ARTIST_ID);

                        // workaround because no group command is allowed in ContentResolver query
                        Set<Long> artistIds = new HashSet<>();

                        do {
                            final String artist = cursor.getString(albumArtistTitleColumnIndex);
                            final long artistId = cursor.getLong(albumIdColumnIndex);

                            // add the artist
                            if (!artistIds.contains(artistId)) {
                                artists.add(new ArtistModel(artist, artistId));
                                artistIds.add(artistId);
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

                        int albumArtistTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionAlbums.ARTIST);

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
     * @deprecated Starting with API Level 30 the support for playlists in the mediastore will end.
     */
    public static List<PlaylistModel> getAllPlaylists(final Context context) {
        final ArrayList<PlaylistModel> playlists = new ArrayList<>();

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, ProjectionPlaylists.PROJECTION, "", null, ProjectionPlaylists.NAME);

        if (cursor != null) {

            if (cursor.moveToFirst()) {
                final int playlistTitleColumnIndex = cursor.getColumnIndexOrThrow(ProjectionPlaylists.NAME);
                final int playlistIdColumnIndex = cursor.getColumnIndexOrThrow(ProjectionPlaylists.ID);

                do {
                    final String playlistTitle = cursor.getString(playlistTitleColumnIndex);
                    final long playlistId = cursor.getLong(playlistIdColumnIndex);

                    // add the playlist
                    playlists.add(new PlaylistModel(playlistTitle, playlistId, PlaylistModel.PLAYLIST_TYPES.MEDIASTORE));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return playlists;
    }

    /**
     * This method returns the storage location for each track that is connected to the provided album key.
     *
     * @param albumId The album id that will be used to get all tracks for this key.
     * @param context The application context to access the content resolver.
     * @return A {@link Set} of all storage locations for each track for the given album key.
     */
    public static Set<String> getTrackStorageLocationsForAlbum(final long albumId, final Context context) {
        final Set<String> trackStorageLocations = new HashSet<>();

        final String[] whereVal = {String.valueOf(albumId), "1"};

        final String where = ProjectionTracks.ALBUM_ID + "=? AND " + ProjectionTracks.IS_MUSIC + "=?";

        final String orderBy = ProjectionTracks.TRACK;

        final Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ProjectionTracks.PROJECTION, where, whereVal, orderBy);

        if (cursor != null) {
            // get all tracks on the current album
            if (cursor.moveToFirst()) {
                do {
                    final String url = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.DATA));

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
    @Nullable
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
                final String title = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.TITLE));
                final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.DURATION));
                final int no = cursor.getInt(cursor.getColumnIndexOrThrow(ProjectionTracks.TRACK));
                final String artist = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST));
                final long artistId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ARTIST_ID));
                final String album = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM));
                final long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ALBUM_ID));
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(ProjectionTracks.ID));

                final Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                track = new TrackModel(title, artist, artistId, album, albumId, duration, no, trackUri, id);
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
                    final String url = cursor.getString(cursor.getColumnIndexOrThrow(ProjectionTracks.DATA));

                    files.add(new FileModel(url));
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return files;
    }
}
