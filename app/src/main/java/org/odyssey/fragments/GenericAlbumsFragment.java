package org.odyssey.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.odyssey.R;
import org.odyssey.adapter.AlbumsGridViewAdapter;
import org.odyssey.listener.OnAlbumSelectedListener;
import org.odyssey.models.AlbumModel;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;
import org.odyssey.utils.ScrollSpeedListener;

import java.util.List;

public abstract class GenericAlbumsFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<AlbumModel>>, AdapterView.OnItemClickListener{

    protected AlbumsGridViewAdapter mAlbumsGridViewAdapter;

    protected OnAlbumSelectedListener mAlbumSelectedCallback;

    protected GridView mRootGrid;

    // Save the last scroll position to resume there
    protected int mLastPosition;

    protected PlaybackServiceConnection mServiceConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.albums_gridview);

        // add progressbar to visualize asynchronous load
        mRootGrid.setEmptyView(rootView.findViewById(R.id.albums_progressbar));

        mAlbumsGridViewAdapter = new AlbumsGridViewAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mAlbumsGridViewAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mAlbumsGridViewAdapter,mRootGrid));
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
            mAlbumSelectedCallback = (OnAlbumSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnAlbumSelectedListener");
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
    public void onLoadFinished(Loader<List<AlbumModel>> loader, List<AlbumModel> data) {
        mAlbumsGridViewAdapter.swapModel(data);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AlbumModel>> loader) {
        mAlbumsGridViewAdapter.swapModel(null);
    }

    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // save last scroll position
        mLastPosition = position;

        // identify current album
        AlbumModel currentAlbum = (AlbumModel) mAlbumsGridViewAdapter.getItem(position);

        String albumKey = currentAlbum.getAlbumKey();
        String albumTitle = currentAlbum.getAlbumName();
        String albumArtURL = currentAlbum.getAlbumArtURL();
        String artistName = currentAlbum.getArtistName();

        // send the event to the host activity
        mAlbumSelectedCallback.onAlbumSelected(albumKey, albumTitle, albumArtURL, artistName);
    }

    protected void enqueueAlbum(int position) {
        // identify current album

        AlbumModel clickedAlbum = (AlbumModel) mAlbumsGridViewAdapter.getItem(position);
        String albumKey = clickedAlbum.getAlbumKey();
        // get and enqueue albumtracks

        String whereVal[] = { albumKey };

        String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

        String orderBy = android.provider.MediaStore.Audio.Media.TRACK;

        Cursor cursor = PermissionHelper.query(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

        if(cursor != null) {
            // get all tracks on the current album
            if (cursor.moveToFirst()) {
                do {
                    String trackName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    int number = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                    String artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url);

                    // enqueue current track
                    try {
                        mServiceConnection.getPBS().enqueueTrack(item);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                } while (cursor.moveToNext());
            }

            cursor.close();
        }
    }

    protected void playAlbum(int position) {
        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get and enqueue albumtracks
        enqueueAlbum(position);

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
