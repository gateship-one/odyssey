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

package org.gateshipone.odyssey.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.util.List;
import java.util.Set;

public interface MusicDatabase {
    List<TrackModel> getTracksForAlbum(final AlbumModel album, final Context context);

    List<TrackModel> getTracksForArtist(final ArtistModel artist, final String orderKey, final Context context);

    List<TrackModel> getTracksForPlaylist(final PlaylistModel playlist, final Context context);

    List<AlbumModel> getRecentAlbums(final Context context);

    List<TrackModel> getRecentTracks(final Context context);

    List<TrackModel> getAllTracks(final String filterString, final Context context);

    void savePlaylist(final String playlistName, final List<TrackModel> tracks, final Context context);

    List<AlbumModel> getAllAlbums(final Context context);

    List<AlbumModel> getAllAlbumsForArtist(final ArtistModel artist, final String orderKey, final Context context);

    List<ArtistModel> getAllArtists(final boolean showAlbumArtistsOnly, final Context context);

    List<PlaylistModel> getAllPlaylists(final Context context);

    boolean removePlaylist(final PlaylistModel playlist, final Context context);

    boolean removeTrackFromPlaylist(final PlaylistModel playlist, final int trackPosition, final Context context);

    TrackModel getTrackForUri(final Uri uri, final Context context);

    List<FileModel> getMediaFilesForPath(final String basePath, final Context context);

    AlbumModel getAlbumForTrack(final TrackModel track, final Context context);

    ArtistModel getArtistForTrack(final TrackModel track, final Context context);

    ArtistModel getArtistForAlbum(final AlbumModel album, final Context context);

    PlaylistModel getPlaylistFromFile(final FileModel file);

    Set<String> getTrackStorageLocationsForAlbum(final AlbumModel album, final Context context);

    /**
     * Create a dummy {@link TrackModel} for the given {@link FileModel}.
     *
     * @param file The given {@link FileModel}.
     * @return A dummy {@link TrackModel} that only contains the file name and the uri.
     */
    TrackModel getDummyTrackModel(FileModel file);

    /**
     * Interface for database abstractions to save their tracks to a SQLite table.
     *
     * @param tracks    List of tracks to be saved
     * @param timestamp timestamp of the track list
     * @param db        Opened! SQLite database object. Does not need to be closed afterwards by abstraction.
     */
    void saveTrackList(List<TrackModel> tracks, long timestamp, SQLiteDatabase db);

    /**
     * Interface for database abstractions to load their tracks from a SQLite table.
     *
     * @param timestamp of the track list to be loaded
     * @param db        Opened! SQLite database object. Does not need to be closed afterwards by abstraction.
     * @return
     */
    List<TrackModel> loadTrackList(long timestamp, SQLiteDatabase db);

    /**
     * Removes a saved track list (from bookmarks or playlist)
     *
     * @param timestamp of the track list to be removed
     * @param db        Opened! SQLite database object. Does not need to be closed afterwards by abstraction.
     */
    void deleteTrackList(long timestamp, SQLiteDatabase db);
}
