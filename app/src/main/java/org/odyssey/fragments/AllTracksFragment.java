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
import android.widget.ListView;

import org.odyssey.R;
import org.odyssey.adapter.TracksListViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;

import java.util.List;

public class AllTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<TrackModel>>, AdapterView.OnItemClickListener {

    private TracksListViewAdapter mTracksListViewAdapter;

    private OnArtistSelectedListener mArtistSelectedCallback;

    private ListView mRootList;

    // Save the last scroll position to resume there
    private int mLastPosition;

    private PlaybackServiceConnection mServiceConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_all_tracks, container, false);

        // get listview
        mRootList = (ListView) rootView.findViewById(R.id.all_tracks_listview);

        // add progressbar
        mRootList.setEmptyView(rootView.findViewById(R.id.all_tracks_progressbar));

        mTracksListViewAdapter = new TracksListViewAdapter(getActivity());

        mRootList.setAdapter(mTracksListViewAdapter);
        mRootList.setOnItemClickListener(this);

        registerForContextMenu(mRootList);

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
    public Loader<List<TrackModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new TrackLoader(getActivity(), "", -1);
    }

    @Override
    public void onLoadFinished(Loader<List<TrackModel>> arg0, List<TrackModel> model) {
        mTracksListViewAdapter.swapModel(model);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootList.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<TrackModel>> arg0) {
        mTracksListViewAdapter.swapModel(null);
    }

    public void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = (TrackModel) mTracksListViewAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        // get artist id
        String whereVal[] = { artistTitle };

        String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

        String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE";

        Cursor artistCursor = getActivity().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, where, whereVal, orderBy);

        if (artistCursor != null) {
            artistCursor.moveToFirst();

            long artistID = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));

            artistCursor.close();

            // Send the event to the host activity
            mArtistSelectedCallback.onArtistSelected(artistTitle, artistID);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_all_tracks_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_all_tracks_action_enqueue:
                enqueueTrack(info.position);
                return true;
            case R.id.fragment_all_tracks_action_enqueueasnext:
                enqueueTrackAsNext(info.position);
                return true;
            case R.id.fragment_all_tracks_action_play:
                playTrack(info.position);
                return true;
            case R.id.fragment_all_tracks_showartist:
                showArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void playTrack(int position) {
        // clear playlist and play current track

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueTrack(position);
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void enqueueTrack(int position) {
        // Enqueue single track

        try {
            TrackModel track = (TrackModel) mTracksListViewAdapter.getItem(position);
            mServiceConnection.getPBS().enqueueTrack(track);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void enqueueTrackAsNext(int position) {
        // Enqueue single track

        try {
            TrackModel track = (TrackModel) mTracksListViewAdapter.getItem(position);
            mServiceConnection.getPBS().enqueueTrackAsNext(track);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playTrack(position);
    }
}
