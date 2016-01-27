package org.odyssey.utils;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;

public class MusicLibraryHelper {
    private static final String TAG = "MusicLibraryHelper";
    public static final String[] projectionAlbums = { MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST };
    public static final String[] projectionArtists = { MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.ARTIST_KEY, MediaStore.Audio.Artists.NUMBER_OF_TRACKS, MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS };
    public static final String[] projectionTracks = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA };
    public static final String[] projectionPlaylistTracks = { MediaStore.Audio.Playlists.Members.TITLE, MediaStore.Audio.Playlists.Members.DISPLAY_NAME, MediaStore.Audio.Playlists.Members.TRACK, MediaStore.Audio.Playlists.Members.ALBUM_KEY,
            MediaStore.Audio.Playlists.Members.DURATION, MediaStore.Audio.Playlists.Members.ALBUM, MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.DATA, MediaStore.Audio.Playlists.Members._ID };

    public static final String[] projectionPlaylists = { MediaStore.Audio.Playlists.NAME, MediaStore.Audio.Playlists._ID };

    public static long getArtistIDFromName(String name, Context context) {
        // get artist id
        long artistID = -1;

        String whereVal[] = { name };

        String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

        String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE";

        Cursor artistCursor = PermissionHelper.query(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, where, whereVal, orderBy);

        if (artistCursor != null) {
            artistCursor.moveToFirst();

            artistID = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));

            artistCursor.close();
        }

        return artistID;
    }

    public static ArrayList<String> getAlbumInformationFromKey(String key, Context context) {
        String whereVal[] = { key };

        String where = MediaStore.Audio.Albums.ALBUM_KEY + "=?";

        Cursor albumCursor = PermissionHelper.query(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, where, whereVal, null);

        ArrayList<String> albumInformations = new ArrayList<>();

        if (albumCursor != null) {
            albumCursor.moveToFirst();

            String albumTitle = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
            String albumArt = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            String artistTitle = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));

            albumInformations.add(albumTitle);
            albumInformations.add(albumArt);
            albumInformations.add(artistTitle);
        }

        return albumInformations;
    }
}
