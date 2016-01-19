package org.odyssey.loaders;

import java.util.ArrayList;
import java.util.List;

import org.odyssey.models.GenericModel;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.models.ArtistModel;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

/*
 * Custom Loader for ARTIST with ALBUM_ART
 */
public class ArtistLoader extends AsyncTaskLoader<List<ArtistModel>> {

    public static boolean SHOW_ONLY_ALBUM_ARTISTS = true;


    Context mContext;

    public ArtistLoader(Context context) {
        super(context);
        this.mContext = context;
    }

    /*
     * Creates an list of all artists on this device and caches them inside the
     * memory(non-Javadoc)
     *
     * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
     */
    @Override
    public List<ArtistModel> loadInBackground() {
        ArrayList<ArtistModel> artists = new ArrayList<ArtistModel>();
        String artist, artistKey, coverPath, albumArtist, albumCoverPath;
        if ( !SHOW_ONLY_ALBUM_ARTISTS ) {
            // get all album covers
            Cursor cursorAlbumArt = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM}, MediaStore.Audio.Albums.ALBUM_ART + "<>\"\" ) GROUP BY (" + MediaStore.Audio.Albums.ARTIST, null,
                    MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC");

            // get all artists
            Cursor cursorArtists = mContext.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE ASC");


            // join both cursor if match is found
            int numberOfTracks, numberOfAlbums;
            long artistID;
            boolean foundCover = false;
            int pos = 0;

            int artistTitleColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
            int artistKeyColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY);
            int artistNoTColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
            int artistIDColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists._ID);
            int artistNoAColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);

            int albumArtistTitleColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
            int albumCoverPathColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

            if (cursorAlbumArt.getCount() > 0) {
                cursorAlbumArt.moveToPosition(0);
            }
            for (int i = 0; i < cursorArtists.getCount(); i++) {
                cursorArtists.moveToPosition(i);

                artist = cursorArtists.getString(artistTitleColumnIndex);
                artistKey = cursorArtists.getString(artistKeyColumnIndex);
                artistID = cursorArtists.getLong(artistIDColumnIndex);

                if (cursorAlbumArt.getString(albumArtistTitleColumnIndex).equals(cursorArtists.getString(artistTitleColumnIndex))) {
                    // Found right album art
                    coverPath = cursorAlbumArt.getString(albumCoverPathColumnIndex);
                    if (!cursorAlbumArt.isLast()) {
                        cursorAlbumArt.moveToNext();
                    }
                } else {
                    coverPath = null;
                }

                artists.add(new ArtistModel(artist, coverPath, artistKey, artistID));

            }

            // return new custom cursor
            cursorAlbumArt.close();
            cursorArtists.close();
        } else {
            // get all album covers
            Cursor cursorAlbumArt = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM}, MediaStore.Audio.Albums.ARTIST + "<>\"\" ) GROUP BY (" + MediaStore.Audio.Albums.ARTIST, null,
                    MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC");

            int albumArtistTitleColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
            int albumCoverPathColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

            for (int i = 0; i < cursorAlbumArt.getCount(); i++) {
                cursorAlbumArt.moveToPosition(i);

                artist = cursorAlbumArt.getString(albumArtistTitleColumnIndex);
                coverPath = cursorAlbumArt.getString(albumCoverPathColumnIndex);

                if ( coverPath == null) {
                    Log.v("ARTISTLOADER", "null coverart :)");
                }

                artists.add(new ArtistModel(artist, coverPath, "", -1));

            }
        }
        return artists;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
