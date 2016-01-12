package org.odyssey.fragments;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.AlbumTracksListViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;
import org.odyssey.utils.MusicLibraryHelper;

import java.util.List;

public class AlbumTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<TrackModel>> {

    private AlbumTracksListViewAdapter mAlbumTracksListViewAdapter;

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

    private ImageView mCoverView;
    private TextView mAlbumTitleView;
    private TextView mAlbumArtistView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_album_tracks, container, false);

        // get listview header
        View headerView = inflater.inflate(R.layout.listview_header_album_tracks, null);

        mCoverView = (ImageView) headerView.findViewById(R.id.header_album_tracks_cover_image);

        mAlbumTitleView = (TextView) headerView.findViewById(R.id.header_album_tracks_album_title);

        mAlbumArtistView = (TextView) headerView.findViewById(R.id.header_album_tracks_artist_name);

        // get listview
        ListView albumTracksListView = (ListView) rootView.findViewById(R.id.album_tracks_listview);

        // FIXME currently disabled, causes crash when backbutton is pressed
        //albumTracksListView.addHeaderView(headerView);

        mAlbumTracksListViewAdapter = new AlbumTracksListViewAdapter(getActivity());

        albumTracksListView.setAdapter(mAlbumTracksListViewAdapter);

        registerForContextMenu(albumTracksListView);

        // set up toolbar
        Bundle args = getArguments();

        mAlbumTitle = args.getString(ARG_ALBUMTITLE);
        mArtistName = args.getString(ARG_ALBUMARTIST);
        mAlbumArtURL = args.getString(ARG_ALBUMART);
        mAlbumKey = args.getString(ARG_ALBUMKEY);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mAlbumTitle, false, false);

        setUpHeaderView();

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

    private void setUpHeaderView() {
        if (mAlbumArtURL != null) {
            if(mAlbumArtURL.equals("")) {
                mCoverView.setImageResource(R.drawable.coverplaceholder);
            } else {
                mCoverView.setImageDrawable(Drawable.createFromPath(mAlbumArtURL));
            }
        } else {
            mCoverView.setImageResource(R.drawable.coverplaceholder);
        }

        mAlbumTitleView.setText(mAlbumTitle);

        mAlbumArtistView.setText(mArtistName);
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mAlbumTitle, false, false);

        setUpHeaderView();

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<List<TrackModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new TrackLoader(getActivity(), mAlbumKey);
    }

    @Override
    public void onLoadFinished(Loader<List<TrackModel>> arg0, List<TrackModel> model) {
        mAlbumTracksListViewAdapter.swapModel(model);
    }

    @Override
    public void onLoaderReset(Loader<List<TrackModel>> arg0) {
        mAlbumTracksListViewAdapter.swapModel(null);
    }

    public void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = (TrackModel) mAlbumTracksListViewAdapter.getItem(position);
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
                Snackbar.make(getActivity().getCurrentFocus(), "add song to playlist: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.fragment_album_tracks_action_enqueueasnext:
                Snackbar.make(getActivity().getCurrentFocus(), "play after current song: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.fragment_album_tracks_action_play:
                Snackbar.make(getActivity().getCurrentFocus(), "play song: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.fragment_album_tracks_action_showartist:
                showArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
