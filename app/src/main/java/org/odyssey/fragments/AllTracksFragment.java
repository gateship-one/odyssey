package org.odyssey.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.odyssey.R;
import org.odyssey.adapter.AllTracksListViewAdapter;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;

import java.util.List;

public class AllTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<TrackModel>> {

    private AllTracksListViewAdapter mAllTracksListViewAdapter;

    private ListView mRootList;

    // Save the last scroll position to resume there
    private int mLastPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_all_tracks, container, false);

        // get gridview
        mRootList = (ListView) rootView.findViewById(R.id.all_tracks_listview);

        // add progressbar
        mRootList.setEmptyView(rootView.findViewById(R.id.all_tracks_progressbar));

        mAllTracksListViewAdapter = new AllTracksListViewAdapter(getActivity());

        mRootList.setAdapter(mAllTracksListViewAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<List<TrackModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new TrackLoader(getActivity());
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

}
