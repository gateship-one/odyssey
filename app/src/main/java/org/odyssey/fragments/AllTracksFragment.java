package org.odyssey.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.odyssey.R;
import org.odyssey.adapter.AllTracksListViewAdapter;

public class AllTracksFragment extends Fragment {

    private AllTracksListViewAdapter mAllTracksListViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_all_tracks, container, false);

        // get gridview
        ListView listView = (ListView) rootView.findViewById(R.id.all_tracks_listview);

        // add progressbar
        listView.setEmptyView(rootView.findViewById(R.id.albums_progressbar));

        mAllTracksListViewAdapter = new AllTracksListViewAdapter(getActivity());

        listView.setAdapter(mAllTracksListViewAdapter);

        return rootView;
    }

}
