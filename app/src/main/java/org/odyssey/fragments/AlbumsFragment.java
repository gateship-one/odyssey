package org.odyssey.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.odyssey.R;
import org.odyssey.adapter.AlbumsGridViewAdapter;

public class AlbumsFragment extends Fragment {

    private AlbumsGridViewAdapter mAlbumsGridViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        // get gridview
        GridView gridView = (GridView) rootView.findViewById(R.id.albums_gridview);

        // add progressbar
        gridView.setEmptyView(rootView.findViewById(R.id.albums_progressbar));

        mAlbumsGridViewAdapter = new AlbumsGridViewAdapter(getActivity(), gridView);

        gridView.setAdapter(mAlbumsGridViewAdapter);

        return rootView;
    }

}
