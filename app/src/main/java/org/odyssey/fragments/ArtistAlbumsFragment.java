package org.odyssey.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.AlbumsGridViewAdapter;
import org.odyssey.listener.OnAlbumSelectedListener;
import org.odyssey.loaders.AlbumLoader;
import org.odyssey.models.AlbumModel;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;
import org.odyssey.utils.ScrollSpeedListener;

import java.util.List;

public class ArtistAlbumsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<AlbumModel>>, AdapterView.OnItemClickListener {

    private AlbumsGridViewAdapter mAlbumsGridViewAdapter;

    private OnAlbumSelectedListener mAlbumSelectedCallback;

    private String mArtistName = "";
    private long mArtistID = -1;

    // FIXME move to separate class to get unified constants?
    public final static String ARG_ARTISTNAME = "artistname";
    public final static String ARG_ARTISTID = "artistid";

    private GridView mRootGrid;

    // Save the last scroll position to resume there
    private int mLastPosition;

    private PlaybackServiceConnection mServiceConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artist_albums, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.artist_albums_gridview);

        // add progressbar to visualize asynchronous load
        mRootGrid.setEmptyView(rootView.findViewById(R.id.artist_albums_progressbar));

        mAlbumsGridViewAdapter = new AlbumsGridViewAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mAlbumsGridViewAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mAlbumsGridViewAdapter, mRootGrid));
        mRootGrid.setOnItemClickListener(this);

        // register for context menu
        registerForContextMenu(mRootGrid);

        // set up toolbar
        Bundle args = getArguments();
        mArtistName = args.getString(ARG_ARTISTNAME);
        mArtistID = args.getLong(ARG_ARTISTID);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mArtistName, false, false);

        // set up play button
        activity.setUpPlayButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAllAlbums();
            }
        });

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
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mArtistName, false, false);

        // set up play button
        activity.setUpPlayButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAllAlbums();
            }
        });

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
    }

    @Override
    public Loader<List<AlbumModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new AlbumLoader(getActivity(), mArtistID);
    }

    @Override
    public void onLoadFinished(Loader<List<AlbumModel>> arg0, List<AlbumModel> model) {
        mAlbumsGridViewAdapter.swapModel(model);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AlbumModel>> arg0) {
        mAlbumsGridViewAdapter.swapModel(null);
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artist_albums_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_artist_albums_action_enqueue:
                enqueueAlbum(info.position);
                return true;
            case R.id.fragment_artist_albums_action_play:
                playAlbum(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void enqueueAlbum(int position) {
        // identify current album

        AlbumModel clickedAlbum = (AlbumModel) mAlbumsGridViewAdapter.getItem(position);
        String albumKey = clickedAlbum.getAlbumKey();
        // get and enqueue albumtracks

        String whereVal[] = { albumKey };

        String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

        String orderBy = android.provider.MediaStore.Audio.Media.TRACK;

        Cursor cursor = PermissionHelper.query(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

        if (cursor != null) {
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

    private void playAlbum(int position) {
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

    private void playAllAlbums() {

        // play all album of current artist if exists

        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get and enqueue albumtracks
        for (int i = 0; i < mAlbumsGridViewAdapter.getCount(); i++) {
            enqueueAlbum(i);
        }

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
