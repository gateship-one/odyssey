package org.odyssey.loaders;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import org.odyssey.models.TrackModel;
import org.odyssey.utils.MusicLibraryHelper;

import java.util.ArrayList;
import java.util.List;

public class TrackLoader extends AsyncTaskLoader<List<TrackModel>> {

    private Context mContext;
    private String mAlbumKey;

    public TrackLoader(Context context, String albumKey) {
        super(context);
        mContext = context;
        mAlbumKey = albumKey;
    }

    /*
     * Creates an list of all albums on this device and caches them inside the
     * memory(non-Javadoc)
     *
     * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
     */
    @Override
    public List<TrackModel> loadInBackground() {
        // Create cursor for content retrieval
        Cursor trackCursor;
        if(mAlbumKey.equals("")) {
            trackCursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, "", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");
        } else {
            String whereVal[] = { mAlbumKey };

            String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

            trackCursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);
        }

        ArrayList<TrackModel> tracks = new ArrayList<TrackModel>();

        int trackTitleColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int trackDurationColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int trackNumberColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int trackArtistColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int trackAlbumColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int trackURLColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int trackAlbumKeyColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);

        for (int i = 0; i < trackCursor.getCount(); i++) {
            trackCursor.moveToPosition(i);

            String title = trackCursor.getString(trackTitleColumnIndex);
            long duration = trackCursor.getLong(trackDurationColumnIndex);
            int no = trackCursor.getInt(trackNumberColumnIndex);
            String artist = trackCursor.getString(trackArtistColumnIndex);
            String album = trackCursor.getString(trackAlbumColumnIndex);
            String url = trackCursor.getString(trackURLColumnIndex);
            String albumKey = trackCursor.getString(trackAlbumKeyColumnIndex);

            TrackModel track = new TrackModel(title, artist, album, albumKey, duration, no, url);
            tracks.add(track);

        }
        trackCursor.close();
        return tracks;
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
