package org.odyssey.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.odyssey.R;
import org.odyssey.adapter.ArtistsGridViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.ArtistLoader;
import org.odyssey.models.ArtistModel;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;
import org.odyssey.utils.ScrollSpeedListener;

import java.util.List;

public class ArtistsFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<ArtistModel>>, AdapterView.OnItemClickListener {

    private ArtistsGridViewAdapter mArtistsGridViewAdapter;

    private OnArtistSelectedListener mArtistSelectedCallback;

    private GridView mRootGrid;

    /**
     * Save the last position here. Gets reused when the user returns to this view after selecting sme
     * albums.
     */
    private int mLastPosition;

    private PlaybackServiceConnection mServiceConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.artists_gridview);

        // add progressbar
        mRootGrid.setEmptyView(rootView.findViewById(R.id.artists_progressbar));

        mArtistsGridViewAdapter = new ArtistsGridViewAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mArtistsGridViewAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mArtistsGridViewAdapter, mRootGrid));
        mRootGrid.setOnItemClickListener(this);

        // register for context menu
        registerForContextMenu(mRootGrid);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mArtistSelectedCallback = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
    }

    @Override
    public Loader<List<ArtistModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new ArtistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ArtistModel>> arg0, List<ArtistModel> model) {
        // Set the actual data to the adapter.
        mArtistsGridViewAdapter.swapModel(model);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ArtistModel>> arg0) {
        // Clear the model data of the adapter.
        mArtistsGridViewAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Save scroll position
        mLastPosition = position;

        // identify current artist
        ArtistModel currentArtist = (ArtistModel) mArtistsGridViewAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        if (artistID == -1 ) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artist, artistID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artists_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_artist_action_enqueue:
                enqueueAllAlbums(info.position);
                return true;
            case R.id.fragment_artist_action_play:
                playAllAlbums(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void enqueueAllAlbums(int position) {

        // identify current artist
        ArtistModel currentArtist = (ArtistModel) mArtistsGridViewAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        if (artistID == -1 ) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // get all albums of the current artist
        Cursor cursorAlbums = PermissionHelper.query(getActivity(), MediaStore.Audio.Artists.Albums.getContentUri("external", artistID), MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");

        String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

        String orderBy = android.provider.MediaStore.Audio.Media.TRACK;

        if(cursorAlbums != null) {
            // get all albums of the current artist
            if (cursorAlbums.moveToFirst()) {
                do {
                    String[] whereVal = {cursorAlbums.getString(cursorAlbums.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY))};

                    Cursor cursorTracks = PermissionHelper.query(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

                    if (cursorTracks != null) {
                        // get all tracks of the current album
                        if (cursorTracks.moveToFirst()) {
                            do {
                                String trackName = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.TITLE));
                                long duration = cursorTracks.getLong(cursorTracks.getColumnIndex(MediaStore.Audio.Media.DURATION));
                                int number = cursorTracks.getInt(cursorTracks.getColumnIndex(MediaStore.Audio.Media.TRACK));
                                String artistName = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                                String albumName = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                                String url = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.DATA));
                                String albumKey = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

                                TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url);

                                // enqueue current track
                                try {
                                    mServiceConnection.getPBS().enqueueTrack(item);
                                } catch (RemoteException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                            } while (cursorTracks.moveToNext());
                        }

                        cursorTracks.close();
                    }

                } while (cursorAlbums.moveToNext());
            }

            cursorAlbums.close();
        }
    }

    private void playAllAlbums(int position) {

        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get and enqueue all albums of the current artist
        enqueueAllAlbums(position);

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }
}
