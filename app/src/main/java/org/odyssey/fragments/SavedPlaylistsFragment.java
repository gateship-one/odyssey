package org.odyssey.fragments;

import android.database.Cursor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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
import org.odyssey.adapter.SavedPlaylistListViewAdapter;
import org.odyssey.loaders.PlaylistLoader;
import org.odyssey.models.PlaylistModel;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;

import java.util.List;

public class SavedPlaylistsFragment extends Fragment implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<List<PlaylistModel>> {

    private SavedPlaylistListViewAdapter mSavedPlaylistListViewAdapter;

    private PlaybackServiceConnection mServiceConnection;

    // Save the last scroll position to resume there
    private int mLastPosition;

    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_saved_playlists, container, false);

        // get listview
        mListView = (ListView) rootView.findViewById(R.id.saved_playlists_listview);

        mSavedPlaylistListViewAdapter = new SavedPlaylistListViewAdapter(getActivity());

        mListView.setOnItemClickListener(this);

        registerForContextMenu(mListView);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_saved_playlists), false, true);

        return rootView;
    }

    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_saved_playlists), false, true);

        // set up pbs connection
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // Save scroll position
        mLastPosition = position;

        // identify current playlist
        PlaylistModel clickedPlaylist = (PlaylistModel) mSavedPlaylistListViewAdapter.getItem(position);

        String playlistName = clickedPlaylist.getPlaylistName();
        long playlistID = clickedPlaylist.getPlaylistID();

        // TODO open playlistfragment
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_saved_playlists_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.saved_playlists_context_menu_action_play:
                playPlaylist(info.position);
                return true;
            case R.id.saved_playlists_context_menu_action_delete:
                deletePlaylist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void playPlaylist(int position) {
        // Remove current playlist
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // identify current playlist
        PlaylistModel clickedPlaylist = (PlaylistModel) mSavedPlaylistListViewAdapter.getItem(position);

        Cursor cursorTracks = getActivity().getContentResolver().query(MediaStore.Audio.Playlists.Members.getContentUri("external", clickedPlaylist.getPlaylistID()), MusicLibraryHelper.projectionPlaylistTracks, "", null, "");


        // get all tracks of the playlist
        if (cursorTracks.moveToFirst()) {
            do {
                String trackName = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE));
                long duration = cursorTracks.getLong(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION));
                int number = cursorTracks.getInt(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
                String artistName = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST));
                String albumName = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM));
                String url = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA));
                String albumKey = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_KEY));

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

        // play playlist
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void deletePlaylist(int position) {
        // identify current playlist
        PlaylistModel clickedPlaylist = (PlaylistModel) mSavedPlaylistListViewAdapter.getItem(position);

        // delete current playlist
        String where = MediaStore.Audio.Playlists._ID + "=?";
        String[] whereVal = { ""+clickedPlaylist.getPlaylistID() };

        getActivity().getContentResolver().delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal);
    }

    @Override
    public Loader<List<PlaylistModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new PlaylistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<PlaylistModel>> arg0, List<PlaylistModel> model) {
        mSavedPlaylistListViewAdapter.swapModel(model);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mListView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<PlaylistModel>> arg0) {
        mSavedPlaylistListViewAdapter.swapModel(null);
    }
}
