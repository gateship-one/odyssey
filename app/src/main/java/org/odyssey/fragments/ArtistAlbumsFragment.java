package org.odyssey.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
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

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.loaders.AlbumLoader;
import org.odyssey.models.AlbumModel;

import java.util.List;

public class ArtistAlbumsFragment extends GenericAlbumsFragment {

    private String mArtistName = "";
    private long mArtistID = -1;

    // FIXME move to separate class to get unified constants?
    public final static String ARG_ARTISTNAME = "artistname";
    public final static String ARG_ARTISTID = "artistid";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        // read arguments
        Bundle args = getArguments();
        mArtistName = args.getString(ARG_ARTISTNAME);
        mArtistID = args.getLong(ARG_ARTISTID);

        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mArtistName, false, false, false);

        // set up play button
        activity.setUpPlayButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAllAlbums();
            }
        });
    }

    @Override
    public Loader<List<AlbumModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new AlbumLoader(getActivity(), mArtistID);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_artist_albums_fragment, menu);

        Drawable drawable = menu.findItem(R.id.action_add_artist_albums).getIcon();

        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(getContext(), R.color.colorTextLight));
        menu.findItem(R.id.action_add_artist_albums).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_artist_albums:
                enqueueAllAlbums();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void enqueueAllAlbums() {
        // iterate over all albums and add to playlist queue
        for (int i = 0; i < mAlbumsGridViewAdapter.getCount(); i++) {
            enqueueAlbum(i);
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
