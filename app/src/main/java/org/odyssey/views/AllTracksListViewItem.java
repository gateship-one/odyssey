package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odyssey.R;

public class AllTracksListViewItem extends LinearLayout{

    private TextView mNumberView;
    private TextView mTitleView;
    private TextView mInformationView;
    private TextView mDurationView;

    public AllTracksListViewItem(Context context, String number, String title, String information, String duration) {
        super(context);

        // TODO check this
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_all_tracks, this, true);

        mNumberView = (TextView) this.findViewById(R.id.item_all_tracks_number);
        mNumberView.setText(number);

        mTitleView = (TextView) this.findViewById(R.id.item_all_tracks_title);
        mTitleView.setText(title);

        mInformationView = (TextView) this.findViewById(R.id.item_all_tracks_additional_information);
        mInformationView.setText(information);

        mDurationView = (TextView) this.findViewById(R.id.item_all_tracks_duration);
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
