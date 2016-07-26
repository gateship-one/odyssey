package org.odyssey.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
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
import org.odyssey.utils.ThemeUtils;

import java.util.List;

public class AlbumTracksFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<TrackModel>>, AdapterView.OnItemClickListener {

    /**
     * ListView adapter object used for this ListView
     */
    private TracksListViewAdapter mTracksListViewAdapter;

    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    /**
     * Key values for arguments of the fragment
     */
    // FIXME move to separate class to get unified constants?
    public final static String ARG_ALBUMKEY = "albumkey";
    public final static String ARG_ALBUMTITLE = "albumtitle";
    public final static String ARG_ALBUMART = "albumart";
    public final static String ARG_ALBUMARTIST = "albumartist";

    /**
     * The information of the displayed album
     */
    private String mAlbumTitle = "";
    private String mAlbumArtURL = "";
    private String mArtistName = "";
    private String mAlbumKey = "";

    /**
     * ServiceConnection object to communicate with the PlaybackService
     */
    private PlaybackServiceConnection mServiceConnection;

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_linear, container, false);

        // get listview
        ListView albumTracksListView = (ListView) rootView.findViewById(R.id.list_linear_listview);

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

        setHasOptionsMenu(true);

        return rootView;
    }

    /**
     * Called when the fragment is first attached to its context.
     */
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

    /**
     * Called when the fragment resumes.
     * Reload the data, setup the toolbar and create the PBS connection.
     */
    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        if (mAlbumArtURL != null) {
            activity.setUpToolbar(mAlbumTitle, false, false, true);
            activity.setToolbarImage(Drawable.createFromPath(mAlbumArtURL));
        } else {
            activity.setUpToolbar(mAlbumTitle, false, false, false);
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

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<TrackModel>> onCreateLoader(int id, Bundle bundle) {
        return new TrackLoader(getActivity(), mAlbumKey, -1);
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param data   Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<TrackModel>> loader, List<TrackModel> data) {
        mTracksListViewAdapter.swapModel(data);
    }

    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<TrackModel>> loader) {
        mTracksListViewAdapter.swapModel(null);
    }

    /**
     * generic method to reload the dataset displayed by the fragment
     */
    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    /**
     * Play the album from the current position.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playAlbum(position);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_album_tracks_fragment, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
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

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_album_tracks_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), android.R.attr.textColor);

        Drawable drawable = menu.findItem(R.id.action_add_album).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_album).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_album:
                enqueueAlbum();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Open a fragment for the artist of the selected album.
     *
     * @param position the position of the selected album in the adapter
     */
    private void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = (TrackModel) mTracksListViewAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, getActivity());

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artistTitle, artistID);
    }

    /**
     * Call the PBS to enqueue the selected track.
     */
    private void enqueueTrack(int position) {
        // Enqueue single track

        try {
            mServiceConnection.getPBS().enqueueTrack((TrackModel) mTracksListViewAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the selected track as next track.
     */
    private void enqueueTrackAsNext(int position) {
        // Enqueue single track

        try {
            mServiceConnection.getPBS().enqueueTrackAsNext((TrackModel) mTracksListViewAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the complete album.
     */
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

    /**
     * Call the PBS to play the complete album and starts with the selected track.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected track in the adapter
     */
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
}
