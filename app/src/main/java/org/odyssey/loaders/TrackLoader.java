package org.odyssey.loaders;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import org.odyssey.models.TrackModel;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.List;

public class TrackLoader extends AsyncTaskLoader<List<TrackModel>> {

    private final Context mContext;
    private final String mAlbumKey;
    private final long mPlaylistID;

    public TrackLoader(Context context, String albumKey, long playlistID) {
        super(context);
        mContext = context;
        mAlbumKey = albumKey;
        mPlaylistID = playlistID;
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

        int trackTitleColumnIndex = -1;
        int trackDurationColumnIndex = -1;
        int trackNumberColumnIndex = -1;
        int trackArtistColumnIndex = -1;
        int trackAlbumColumnIndex = -1;
        int trackURLColumnIndex = -1;
        int trackAlbumKeyColumnIndex = -1;
        int trackIdColumnIndex = -1;

        if(mPlaylistID != -1) {
            // load playlist tracks
            trackCursor = PermissionHelper.query(mContext, MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID), MusicLibraryHelper.projectionPlaylistTracks, "", null, "");

            if(trackCursor != null) {
                trackTitleColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE);
                trackDurationColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION);
                trackNumberColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK);
                trackArtistColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST);
                trackAlbumColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM);
                trackURLColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA);
                trackAlbumKeyColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_KEY);
                trackIdColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
            }
        } else {

            if (mAlbumKey.equals("")) {
                // load all tracks
                trackCursor = PermissionHelper.query(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, "", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");
            } else {
                // load album tracks

                String whereVal[] = {mAlbumKey};

                String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

                trackCursor = PermissionHelper.query(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);
            }

            if(trackCursor != null) {
                trackTitleColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                trackDurationColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                trackNumberColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
                trackArtistColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                trackAlbumColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                trackURLColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                trackAlbumKeyColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);
                trackIdColumnIndex = trackCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            }
        }

        ArrayList<TrackModel> tracks = new ArrayList<>();

        if(trackCursor != null) {
            for (int i = 0; i < trackCursor.getCount(); i++) {
                trackCursor.moveToPosition(i);

                String title = trackCursor.getString(trackTitleColumnIndex);
                long duration = trackCursor.getLong(trackDurationColumnIndex);
                int no = trackCursor.getInt(trackNumberColumnIndex);
                String artist = trackCursor.getString(trackArtistColumnIndex);
                String album = trackCursor.getString(trackAlbumColumnIndex);
                String url = trackCursor.getString(trackURLColumnIndex);
                String albumKey = trackCursor.getString(trackAlbumKeyColumnIndex);
                long id = trackCursor.getLong(trackIdColumnIndex);

                TrackModel track = new TrackModel(title, artist, album, albumKey, duration, no, url, id);
                tracks.add(track);

            }
            trackCursor.close();
        }

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
