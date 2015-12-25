package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odyssey.R;

public class CurrentPlaylistViewItem extends LinearLayout{

    private TextView mNumberView;
    private TextView mTitleView;
    private TextView mInformationView;
    private TextView mDurationView;

    public CurrentPlaylistViewItem(Context context, String number, String title, String information, String duration) {
        super(context);

        View rootView = LayoutInflater.from(context).inflate(R.layout.listview_item_current_playlist, this, true);

        mNumberView = (TextView) rootView.findViewById(R.id.item_current_playlist_number);
        mNumberView.setText(number);

        mTitleView = (TextView) rootView.findViewById(R.id.item_current_playlist_title);
        mTitleView.setText(title);

        mInformationView = (TextView) rootView.findViewById(R.id.item_current_playlist_additional_information);
        mInformationView.setText(information);

        mDurationView = (TextView) rootView.findViewById(R.id.item_current_playlist_duration);
        mDurationView.setText(duration);
    }

    public void setNumber(String number) {
        mNumberView.setText(number);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    public void setAdditionalInformation(String information) {
        mInformationView.setText(information);
    }

    public void setDuration(String duration) {
        mDurationView.setText(duration);
    }
}
