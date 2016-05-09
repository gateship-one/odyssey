package org.odyssey.fragments;

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
import android.widget.ListView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.TracksListViewAdapter;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;

import java.util.List;

public class PlaylistTracksFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<TrackModel>>, AdapterView.OnItemClickListener{

    private TracksListViewAdapter mTracksListViewAdapter;

    private PlaybackServiceConnection mServiceConnection;

    // FIXME move to separate class to get unified constants?
    public final static String ARG_PLAYLISTTITLE = "playlisttitle";
    public final static String ARG_PLAYLISTID = "playlistid";

    private String mPlaylistTitle = "";
    private long mPlaylistID = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_playlist_tracks, container, false);

        // get listview
        ListView playlistTracksListView = (ListView) rootView.findViewById(R.id.playlist_tracks_listview);

        mTracksListViewAdapter = new TracksListViewAdapter(getActivity());

        playlistTracksListView.setAdapter(mTracksListViewAdapter);

        playlistTracksListView.setOnItemClickListener(this);

        registerForContextMenu(playlistTracksListView);

        Bundle args = getArguments();

        mPlaylistTitle = args.getString(ARG_PLAYLISTTITLE);
        mPlaylistID = args.getLong(ARG_PLAYLISTID);

        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mPlaylistTitle, false, false,false);

        // set up play button
        activity.setUpPlayButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPlaylist(0);
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mPlaylistTitle, false, false,false);

        // set up play button
        activity.setUpPlayButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPlaylist(0);
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
        return new TrackLoader(getActivity(), "", mPlaylistID);
    }

    @Override
    public void onLoadFinished(Loader<List<TrackModel>> arg0, List<TrackModel> model) {
        mTracksListViewAdapter.swapModel(model);
    }

    @Override
    public void onLoaderReset(Loader<List<TrackModel>> arg0) {
        mTracksListViewAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playPlaylist(position);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_playlist_tracks_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_playlist_tracks_action_enqueue:
                enqueueTrack(info.position);
                return true;
            case R.id.fragment_playlist_tracks_action_enqueueasnext:
                enqueueTrackAsNext(info.position);
                return true;
            case R.id.fragment_playlist_tracks_action_remove:
                removeTrackFromPlaylist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    void playPlaylist(int position) {
        // clear playlist and play current album

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueuePlaylist();
            mServiceConnection.getPBS().jumpTo(position);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    void enqueuePlaylist() {
        // enqueue complete playlist

        for (int i = 0; i < mTracksListViewAdapter.getCount(); i++) {
            try {
                TrackModel track = (TrackModel) mTracksListViewAdapter.getItem(i);
                mServiceConnection.getPBS().enqueueTrack(track);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    void enqueueTrack(int position) {
        // Enqueue single track

        try {
            TrackModel track = (TrackModel) mTracksListViewAdapter.getItem(position);
            mServiceConnection.getPBS().enqueueTrack(track);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void enqueueTrackAsNext(int position) {
        // Enqueue single track as next

        try {
            TrackModel track = (TrackModel) mTracksListViewAdapter.getItem(position);
            mServiceConnection.getPBS().enqueueTrackAsNext(track);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void removeTrackFromPlaylist(int position) {
        Cursor trackCursor = PermissionHelper.query(getActivity(), MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID), MusicLibraryHelper.projectionPlaylistTracks, "", null, "");

        if (trackCursor != null) {
            if (trackCursor.moveToPosition(position)) {
                String where = MediaStore.Audio.Playlists.Members._ID + "=?";
                String[] whereVal = {trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID))};

                PermissionHelper.delete(getActivity(), MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID), where, whereVal);

                // reload data
                getLoaderManager().restartLoader(0, getArguments(), this);
            }

            trackCursor.close();
        }
    }

    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }
}
