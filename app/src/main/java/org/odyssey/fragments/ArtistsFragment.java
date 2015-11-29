package org.odyssey.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.odyssey.R;
import org.odyssey.adapter.ArtistsGridViewAdapter;

public class ArtistsFragment extends Fragment {

    private ArtistsGridViewAdapter mArtistsGridViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        // get gridview
        GridView gridView = (GridView) rootView.findViewById(R.id.artists_gridview);

        // add progressbar
        gridView.setEmptyView(rootView.findViewById(R.id.artists_progressbar));

        mArtistsGridViewAdapter = new ArtistsGridViewAdapter(getActivity(), gridView);

        gridView.setAdapter(mArtistsGridViewAdapter);

        return rootView;
    }

}
