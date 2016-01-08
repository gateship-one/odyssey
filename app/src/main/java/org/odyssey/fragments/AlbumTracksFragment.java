package org.odyssey.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.odyssey.R;
import org.odyssey.adapter.AlbumTracksListViewAdapter;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;

import java.util.List;

public class AlbumTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<TrackModel>> {

    private AlbumTracksListViewAdapter mAlbumTracksListViewAdapter;

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

        return rootView;
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

        getActivity().setTitle(mAlbumTitle);

        setUpHeaderView();

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<List<TrackModel>> onCreateLoader(int arg0, Bundle bundle) {
        mAlbumTitle = bundle.getString(ARG_ALBUMTITLE);
        mArtistName = bundle.getString(ARG_ALBUMARTIST);
        mAlbumArtURL = bundle.getString(ARG_ALBUMART);
        mAlbumKey = bundle.getString(ARG_ALBUMKEY);

        getActivity().setTitle(mAlbumTitle);

        setUpHeaderView();

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
}
