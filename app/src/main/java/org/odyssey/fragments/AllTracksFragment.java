package org.odyssey.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
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
import org.odyssey.adapter.AllTracksListViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;
import org.odyssey.utils.MusicLibraryHelper;

import java.util.List;

public class AllTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<TrackModel>> {

    private AllTracksListViewAdapter mAllTracksListViewAdapter;

    private OnArtistSelectedListener mArtistSelectedCallback;

    private ListView mRootList;

    // Save the last scroll position to resume there
    private int mLastPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_all_tracks, container, false);

        // get listview
        mRootList = (ListView) rootView.findViewById(R.id.all_tracks_listview);

        // add progressbar
        mRootList.setEmptyView(rootView.findViewById(R.id.all_tracks_progressbar));

        mAllTracksListViewAdapter = new AllTracksListViewAdapter(getActivity());

        mRootList.setAdapter(mAllTracksListViewAdapter);

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
    }

    @Override
    public Loader<List<TrackModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new TrackLoader(getActivity(), "");
    }

    @Override
    public void onLoadFinished(Loader<List<TrackModel>> arg0, List<TrackModel> model) {
        mAllTracksListViewAdapter.swapModel(model);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootList.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<TrackModel>> arg0) {
        mAllTracksListViewAdapter.swapModel(null);
    }

    public void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = (TrackModel) mAllTracksListViewAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        // get artist id
        String whereVal[] = { artistTitle };

        String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

        String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE";

        Cursor artistCursor = getActivity().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, where, whereVal, orderBy);

        artistCursor.moveToFirst();

        long artistID = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artistTitle, artistID);
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
                Snackbar.make(getActivity().getCurrentFocus(), "add song to playlist: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.fragment_all_tracks_action_enqueueasnext:
                Snackbar.make(getActivity().getCurrentFocus(), "play after current song: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.fragment_all_tracks_action_play:
                Snackbar.make(getActivity().getCurrentFocus(), "play song: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.fragment_all_tracks_showartist:
                showArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
