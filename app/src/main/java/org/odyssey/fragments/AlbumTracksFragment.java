package org.odyssey.fragments;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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
import android.widget.ListView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.TracksListViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;

import java.util.List;

public class AlbumTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<TrackModel>>, AdapterView.OnItemClickListener {

    private TracksListViewAdapter mTracksListViewAdapter;

    private OnArtistSelectedListener mArtistSelectedCallback;

    // FIXME move to separate class to get unified constants?
    public final static String ARG_ALBUMKEY = "albumkey";
    public final static String ARG_ALBUMTITLE = "albumtitle";
    public final static String ARG_ALBUMART = "albumart";
    public final static String ARG_ALBUMARTIST = "albumartist";

    private String mAlbumTitle = "";
    private String mAlbumArtURL = "";
    private String mArtistName = "";
    private String mAlbumKey = "";

    private PlaybackServiceConnection mServiceConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_album_tracks, container, false);

        // get listview
        ListView albumTracksListView = (ListView) rootView.findViewById(R.id.album_tracks_listview);

        mTracksListViewAdapter = new TracksListViewAdapter(getActivity());

        albumTracksListView.setAdapter(mTracksListViewAdapter);

        albumTracksListView.setOnItemClickListener(this);

        registerForContextMenu(albumTracksListView);

        // set up toolbar
        Bundle args = getArguments();

        mAlbumTitle = args.getString(ARG_ALBUMTITLE);
        mArtistName = args.getString(ARG_ALBUMARTIST);
        mAlbumArtURL = args.getString(ARG_ALBUMART);
        mAlbumKey = args.getString(ARG_ALBUMKEY);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();


        if ( mAlbumArtURL != null ) {
            activity.setUpToolbar(mAlbumTitle, false, false,true);
            activity.setToolbarImage(Drawable.createFromPath(mAlbumArtURL));
        } else {
            activity.setUpToolbar(mAlbumTitle, false, false,false);
        }

        // set up play button
        activity.setUpPlayButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAlbum(0);
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
            mArtistSelectedCallback = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        if ( mAlbumArtURL != null ) {
            activity.setUpToolbar(mAlbumTitle, false, false,true);
            activity.setToolbarImage(Drawable.createFromPath(mAlbumArtURL));
        } else {
            activity.setUpToolbar(mAlbumTitle, false, false,false);
        }

        // set up play button
        activity.setUpPlayButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAlbum(0);
            }
        });

        // set up pbs connection
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<List<TrackModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new TrackLoader(getActivity(), mAlbumKey, -1);
    }

    @Override
    public void onLoadFinished(Loader<List<TrackModel>> arg0, List<TrackModel> model) {
        mTracksListViewAdapter.swapModel(model);
    }

    @Override
    public void onLoaderReset(Loader<List<TrackModel>> arg0) {
        mTracksListViewAdapter.swapModel(null);
    }

    public void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = (TrackModel) mTracksListViewAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, getActivity());

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artistTitle, artistID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_album_tracks_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_album_tracks_action_enqueue:
                enqueueTrack(info.position);
                return true;
            case R.id.fragment_album_tracks_action_enqueueasnext:
                enqueueTrackAsNext(info.position);
                return true;
            case R.id.fragment_album_tracks_action_play:
                playAlbum(info.position);
                return true;
            case R.id.fragment_album_tracks_action_showartist:
                showArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void enqueueTrack(int position) {
        // Enqueue single track

        try {
            mServiceConnection.getPBS().enqueueTrack((TrackModel) mTracksListViewAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void enqueueTrackAsNext(int position) {
        // Enqueue single track

        try {
            mServiceConnection.getPBS().enqueueTrackAsNext((TrackModel) mTracksListViewAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void playAlbum(int position) {
        // clear playlist and play current album

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueAlbum();
            mServiceConnection.getPBS().jumpTo(position);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void enqueueAlbum() {
        // Enqueue complete album

        // enqueue albumtracks
        for (int i = 0; i < mTracksListViewAdapter.getCount(); i++) {
            try {
                mServiceConnection.getPBS().enqueueTrack((TrackModel) mTracksListViewAdapter.getItem(i));
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playAlbum(position);
    }
}
