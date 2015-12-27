package org.odyssey.loaders;

import java.util.ArrayList;
import java.util.List;

import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.models.AlbumModel;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;

public class AlbumLoader extends AsyncTaskLoader<List<AlbumModel>> {

    private final static String TAG = "OdysseyAlbumLoader";
    private long mArtistID;
    private Context mContext;

    public AlbumLoader(Context context, long artist) {
        super(context);
        mContext = context;
        mArtistID = artist;
    }

    /*
     * Creates an list of all albums on this device and caches them inside the
     * memory(non-Javadoc)
     * 
     * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
     */
    @Override
    public List<AlbumModel> loadInBackground() {
        // Create cursor for content retrieval
        Cursor albumCursor;
        if (mArtistID == -1) {
            albumCursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");
        } else {
            albumCursor = mContext.getContentResolver().query(MediaStore.Audio.Artists.Albums.getContentUri("external", mArtistID), MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");
        }
        ArrayList<AlbumModel> albums = new ArrayList<AlbumModel>();

        int albumKeyColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
        int albumTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
        int imagePathColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
        int artistTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);

        for (int i = 0; i < albumCursor.getCount(); i++) {
            albumCursor.moveToPosition(i);
            String albumKey = albumCursor.getString(albumKeyColumnIndex);
            String albumTitle = albumCursor.getString(albumTitleColumnIndex);
            String imagePath = albumCursor.getString(imagePathColumnIndex);
            // if (imagePath == null || imagePath.equals("")) {
            // Log.v(TAG, "Album: " + albumTitle);
            // }
            String artistTitle = albumCursor.getString(artistTitleColumnIndex);
            AlbumModel album = new AlbumModel(albumTitle, imagePath, artistTitle, albumKey);
            albums.add(album);

        }
        albumCursor.close();
        return albums;
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