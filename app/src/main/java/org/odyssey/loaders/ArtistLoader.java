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

/*
 * Custom Loader for ARTIST with ALBUM_ART
 */
public class ArtistLoader extends AsyncTaskLoader<List<GenericModel>> {

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
    public List<GenericModel> loadInBackground() {

        // get all album covers
        Cursor cursorAlbumArt = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM }, "", null,
                MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE");

        // get all artists
        Cursor cursorArtists = mContext.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE");

        ArrayList<GenericModel> artists = new ArrayList<GenericModel>();

        // join both cursor if match is found
        String artist, artistKey, coverPath, albumArtist, albumCoverPath;
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

        CursorJoiner cursorArtistsWithArt = new CursorJoiner(cursorAlbumArt, new String[] { MediaStore.Audio.Albums.ARTIST }, cursorArtists, new String[] { MediaStore.Audio.Artists.ARTIST });

        for (CursorJoiner.Result result : cursorArtistsWithArt) {
            switch (result) {
                case LEFT:
                    // handle case where a row in cursorAlbumArt is unique
                    // this case should never occur
                    break;
                case RIGHT:
                    // handle case where a row in cursorArtists is unique
                    artist = cursorArtists.getString(artistTitleColumnIndex);
                    artistKey = cursorArtists.getString(artistKeyColumnIndex);
                    artistID = cursorArtists.getLong(artistIDColumnIndex);
                    coverPath = null;
                    artists.add(new ArtistModel(artist, coverPath, artistKey, artistID));
                    break;
                case BOTH:
                    // handle case where a row with the same key is in both cursors
                    artist = cursorArtists.getString(artistTitleColumnIndex);
                    artistKey = cursorArtists.getString(artistKeyColumnIndex);
                    artistID = cursorArtists.getLong(artistIDColumnIndex);
                    coverPath = cursorAlbumArt.getString(albumCoverPathColumnIndex);
                    artists.add(new ArtistModel(artist, coverPath, artistKey, artistID));
                    break;
            }
        }

        // return new custom cursor
        cursorAlbumArt.close();
        cursorArtists.close();
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
