package org.odyssey.loaders;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import org.odyssey.models.PlaylistModel;
import org.odyssey.utils.MusicLibraryHelper;

import java.util.ArrayList;
import java.util.List;

public class PlaylistLoader extends AsyncTaskLoader<List<PlaylistModel>> {

    private Context mContext;

    public PlaylistLoader(Context context) {
        super(context);

        mContext = context;
    }

    /*
     * Creates an list of all albums on this device and caches them inside the
     * memory(non-Javadoc)
     *
     * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
     */
    @Override
    public List<PlaylistModel> loadInBackground() {
        Cursor playlistCursor =  mContext.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionPlaylists, "", null, MediaStore.Audio.Playlists.NAME);

        ArrayList<PlaylistModel> playlists = new ArrayList<PlaylistModel>();

        int playlistTitleColumnIndex = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);
        int playlistIDColumnIndex = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._ID);

        for (int i = 0; i < playlistCursor.getCount(); i++) {
            playlistCursor.moveToPosition(i);

            String playlistTitle = playlistCursor.getString(playlistTitleColumnIndex);
            long playlistID = playlistCursor.getLong(playlistIDColumnIndex);

            PlaylistModel playlist = new PlaylistModel(playlistTitle, playlistID);
            playlists.add(playlist);
        }

        playlistCursor.close();
        return playlists;
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
