package org.odyssey.loaders;

import java.util.ArrayList;
import java.util.List;

import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.models.AlbumModel;
import org.odyssey.utils.PermissionHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class AlbumLoader extends AsyncTaskLoader<List<AlbumModel>> {

    private final static String TAG = "OdysseyAlbumLoader";
    private long mArtistID;
    private Context mContext;
    private String mOrderBy;

    public AlbumLoader(Context context, long artist) {
        super(context);
        mContext = context;
        mArtistID = artist;

        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String orderPref = sharedPref.getString("pref_album_sort_order","name");

        switch (orderPref) {
            case "name":
                mOrderBy = MediaStore.Audio.Albums.ALBUM;
                break;
            case "year":
                mOrderBy = MediaStore.Audio.Albums.FIRST_YEAR;
                break;
        }
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
            albumCursor = PermissionHelper.query(mContext, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");
        } else {
            albumCursor = PermissionHelper.query(mContext, MediaStore.Audio.Artists.Albums.getContentUri("external", mArtistID), MusicLibraryHelper.projectionAlbums, "", null, mOrderBy + " COLLATE NOCASE");
        }
        ArrayList<AlbumModel> albums = new ArrayList<AlbumModel>();

        if(albumCursor != null) {
            int albumKeyColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY);
            int albumTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
            int imagePathColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
            int artistTitleColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);

            for (int i = 0; i < albumCursor.getCount(); i++) {
                albumCursor.moveToPosition(i);
                String albumKey = albumCursor.getString(albumKeyColumnIndex);
                String albumTitle = albumCursor.getString(albumTitleColumnIndex);
                String imagePath = albumCursor.getString(imagePathColumnIndex);
                String artistTitle = albumCursor.getString(artistTitleColumnIndex);
                AlbumModel album = new AlbumModel(albumTitle, imagePath, artistTitle, albumKey);
                albums.add(album);

            }
            albumCursor.close();
        }
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