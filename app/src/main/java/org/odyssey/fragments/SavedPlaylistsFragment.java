package org.odyssey.fragments;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.R;

public class SavedPlaylistsFragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_saved_playlists, container, false);

        // set title
        getActivity().setTitle(R.string.fragment_title_saved_playlists);

        return rootView;
    }

}
