package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odyssey.R;

public class NowPlayingView extends LinearLayout{
    public NowPlayingView(Context context) {
        super(context);

        View rootView = LayoutInflater.from(context).inflate(R.layout.view_now_playing, this, true);

        // FIXME
        TextView trackTitle = (TextView) rootView.findViewById(R.id.now_playing_trackTitle);
        trackTitle.setText("Dummy Title");

        TextView trackArtist = (TextView) rootView.findViewById(R.id.now_playing_trackArtist);
        trackArtist.setText("Jack O'Neill");

        TextView trackAlbum = (TextView) rootView.findViewById(R.id.now_playing_trackAlbum);
        trackAlbum.setText("Stargate SG-1");

        TextView elapsedTime = (TextView) rootView.findViewById(R.id.now_playing_elapsedTime);
        elapsedTime.setText("47:11");

        TextView length = (TextView) rootView.findViewById(R.id.now_playing_length);
        length.setText("42:00");
    }
}
