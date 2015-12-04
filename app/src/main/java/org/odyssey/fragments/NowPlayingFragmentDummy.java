package org.odyssey.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.odyssey.R;
import org.odyssey.views.NowPlayingView;

public class NowPlayingFragmentDummy extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_now_playing_removeme, container, false);

        View nowPlaying = new NowPlayingView(getContext());

        LinearLayout lL = (LinearLayout)rootView.findViewById(R.id.now_playing_layout_dummy);

        lL.addView(nowPlaying);

        return rootView;
    }

}
