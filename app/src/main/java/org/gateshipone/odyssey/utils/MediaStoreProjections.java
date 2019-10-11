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

package org.gateshipone.odyssey.utils;

import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

/**
 * Class that contains several arrays that can be used to access different informations from the {@link MediaStore}.
 */
class MediaStoreProjections {

    interface ProjectionAlbums {
        /**
         * projection array
         */
        String[] PROJECTION = getProjectionAlbums();

        /**
         * fast access to projection entries
         */
        String ALBUM = PROJECTION[0];
        String ALBUM_KEY = PROJECTION[1];
        String NUMER_OF_SONGS = PROJECTION[2];
        String ALBUM_ART = PROJECTION[3];
        String ARTIST = PROJECTION[4];
        String FIRST_YEAR = PROJECTION[5];
        String LAST_YEAR = PROJECTION[6];
        String ID = PROJECTION[7];
        @RequiresApi(api = Build.VERSION_CODES.Q)
        String ARTIST_ID = PROJECTION[8];
    }

    interface ProjectionArtists {
        /**
         * projection array
         */
        String[] PROJECTION = getProjectionArtists();

        /**
         * fast access to projection entries
         */
        String ARTIST = PROJECTION[0];
        String NUMBER_OF_TRACKS = PROJECTION[1];
        String NUMBER_OF_ALBUMS = PROJECTION[2];
        String ID = PROJECTION[3];
    }

    interface ProjectionTracks {
        /**
         * projection array
         */
        String[] PROJECTION = getProjectionTracks();

        /**
         * fast access to projection entries
         */
        String TITLE = PROJECTION[0];
        String DISPLAY_NAME = PROJECTION[1];
        String TRACK = PROJECTION[2];
        String ALBUM_KEY = PROJECTION[3];
        String ALBUM = PROJECTION[4];
        String ARTIST = PROJECTION[5];
        String DATA = PROJECTION[6];
        String DATE_ADDED = PROJECTION[7];
        String DURATION = PROJECTION[8];
        String ID = PROJECTION[9];

        String IS_MUSIC = MediaStore.Audio.Media.IS_MUSIC;
    }

    interface ProjectionPlaylists {
        /**
         * projection array
         */
        String[] PROJECTION = getProjectionPlaylists();

        /**
         * fast access to projection entries
         */
        String NAME = PROJECTION[0];
        String ID = PROJECTION[1];

        String DATE_ADDED = MediaStore.Audio.Playlists.DATE_ADDED;
        String DATE_MODIFIED = MediaStore.Audio.Playlists.DATE_MODIFIED;
    }

    interface ProjectionPlaylistTracks {
        /**
         * projection array
         */
        String[] PROJECTION = getProjectionPlaylistTracks();

        /**
         * fast access to projection entries
         */
        String TITLE = PROJECTION[0];
        String DISPLAY_NAME = PROJECTION[1];
        String TRACK = PROJECTION[2];
        String ALBUM_KEY = PROJECTION[3];
        String ALBUM = PROJECTION[4];
        String ARTIST = PROJECTION[5];
        String DATA = PROJECTION[6];
        String ID = PROJECTION[7];
        String AUDIO_ID = PROJECTION[8];
        String DURATION = PROJECTION[9];

        String PLAY_ORDER = MediaStore.Audio.Playlists.Members.PLAY_ORDER;
    }

    private static String[] getProjectionAlbums() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ALBUM_KEY,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                    MediaStore.Audio.Albums.ALBUM_ART,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.FIRST_YEAR,
                    MediaStore.Audio.Albums.LAST_YEAR,
                    MediaStore.Audio.Albums.ALBUM_ID,
                    MediaStore.Audio.Albums.ARTIST_ID
            };
        } else {
            return new String[]{
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ALBUM_KEY,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                    MediaStore.Audio.Albums.ALBUM_ART,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.FIRST_YEAR,
                    MediaStore.Audio.Albums.LAST_YEAR,
                    BaseColumns._ID,
            };
        }
    }

    private static String[] getProjectionArtists() {
        return new String[]{
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                BaseColumns._ID,
        };
    }

    private static String[] getProjectionTracks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.ALBUM_KEY,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.MediaColumns.DURATION,
                    BaseColumns._ID,
            };
        } else {
            return new String[]{
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.ALBUM_KEY,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.AudioColumns.DURATION,
                    BaseColumns._ID,
            };
        }
    }

    private static String[] getProjectionPlaylists() {
        return new String[]{
                MediaStore.Audio.Playlists.NAME,
                BaseColumns._ID
        };
    }

    private static String[] getProjectionPlaylistTracks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{
                    MediaStore.Audio.Playlists.Members.TITLE,
                    MediaStore.Audio.Playlists.Members.DISPLAY_NAME,
                    MediaStore.Audio.Playlists.Members.TRACK,
                    MediaStore.Audio.Playlists.Members.ALBUM_KEY,
                    MediaStore.Audio.Playlists.Members.ALBUM,
                    MediaStore.Audio.Playlists.Members.ARTIST,
                    MediaStore.Audio.Playlists.Members.DATA,
                    MediaStore.Audio.Playlists.Members._ID,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    MediaStore.MediaColumns.DURATION
            };
        } else {
            return new String[]{
                    MediaStore.Audio.Playlists.Members.TITLE,
                    MediaStore.Audio.Playlists.Members.DISPLAY_NAME,
                    MediaStore.Audio.Playlists.Members.TRACK,
                    MediaStore.Audio.Playlists.Members.ALBUM_KEY,
                    MediaStore.Audio.Playlists.Members.ALBUM,
                    MediaStore.Audio.Playlists.Members.ARTIST,
                    MediaStore.Audio.Playlists.Members.DATA,
                    MediaStore.Audio.Playlists.Members._ID,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    MediaStore.Audio.AudioColumns.DURATION,
            };
        }
    }
}
