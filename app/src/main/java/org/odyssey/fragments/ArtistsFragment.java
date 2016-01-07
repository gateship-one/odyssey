package org.odyssey.fragments;

import android.content.Context;
import android.support.v4.app.LoaderManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.odyssey.R;
import org.odyssey.adapter.ArtistsGridViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.ArtistLoader;
import org.odyssey.models.ArtistModel;
import org.odyssey.models.GenericModel;
import org.odyssey.utils.ScrollSpeedListener;

import java.util.List;

public class ArtistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ArtistModel>>, AdapterView.OnItemClickListener {

    private ArtistsGridViewAdapter mArtistsGridViewAdapter;

    private OnArtistSelectedListener mArtistSelectedCallback;

    private GridView mRootGrid;

    // Save the last scroll position to resume there
    private int mLastPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.artists_gridview);

        // add progressbar
        mRootGrid.setEmptyView(rootView.findViewById(R.id.artists_progressbar));

        mArtistsGridViewAdapter = new ArtistsGridViewAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mArtistsGridViewAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mArtistsGridViewAdapter, mRootGrid));
        mRootGrid.setOnItemClickListener(this);

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
    public Loader<List<ArtistModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new ArtistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ArtistModel>> arg0, List<ArtistModel> model) {
        mArtistsGridViewAdapter.swapModel(model);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ArtistModel>> arg0) {
        mArtistsGridViewAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Save scroll position
        mLastPosition = position;

        // identify current artist
        ArtistModel currentArtist = (ArtistModel) mArtistsGridViewAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        // send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artist, artistID);
    }
}
