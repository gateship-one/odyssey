package org.odyssey.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.odyssey.R;
import org.odyssey.adapter.AlbumsGridViewAdapter;
import org.odyssey.loaders.AlbumLoader;
import org.odyssey.models.AlbumModel;

import java.util.List;

public class AlbumsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<AlbumModel>> {

    private AlbumsGridViewAdapter mAlbumsGridViewAdapter;

    private String mArtistName = "";
    private long mArtistID = -1;

    // FIXME move to separate class to get unified constants?
    public final static String ARG_ARTISTNAME = "artistname";
    public final static String ARG_ARTISTID = "artistid";

    private GridView mRootGrid;

    // Save the last scroll position to resume there
    private int mLastPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.albums_gridview);

        // add progressbar
        mRootGrid.setEmptyView(rootView.findViewById(R.id.albums_progressbar));

        mAlbumsGridViewAdapter = new AlbumsGridViewAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mAlbumsGridViewAdapter);

        getLoaderManager().initLoader(0, getArguments(), this);
        return rootView;
    }
    @Override
    public Loader<List<AlbumModel>> onCreateLoader(int arg0, Bundle bundle) {
        if (bundle == null) {

            // all albums
            return new AlbumLoader(getActivity(), -1);

        } else {

            // only albums of artist mArtist

            mArtistName = bundle.getString(ARG_ARTISTNAME);
            mArtistID = bundle.getLong(ARG_ARTISTID);

            // Set actionbar title
            getActivity().getActionBar().setTitle(mArtistName);

            return new AlbumLoader(getActivity(), mArtistID);
        }
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
}
