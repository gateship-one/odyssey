package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odyssey.R;

public class TracksListViewItem extends LinearLayout {

    protected final TextView mNumberView;
    protected final TextView mTitleView;
    protected final TextView mInformationView;
    protected final TextView mDurationView;

    /**
     * Constructor that only initialize the layout.
     */
    public TracksListViewItem(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_tracks, this, true);

        mTitleView = (TextView) findViewById(R.id.item_tracks_title);
        mNumberView = (TextView) findViewById(R.id.item_tracks_number);
        mInformationView = (TextView) findViewById(R.id.item_tracks_additional_information);
        mDurationView = (TextView) findViewById(R.id.item_tracks_duration);
    }

    /**
     * Constructor that already sets the values for each view.
     */
    public TracksListViewItem(Context context, String number, String title, String information, String duration) {
        this(context);

        mTitleView.setText(title);
        mNumberView.setText(number);
        mInformationView.setText(information);
        mDurationView.setText(duration);
    }

    /**
     * Sets the title for the ListItem.
     */
    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the number text for the ListItem.
     */
    public void setNumber(String number) {
        mNumberView.setText(number);
    }

    /**
     * Sets the additional information text for the ListItem.
     * For example a combination text of artist and album for the representing track.
     */
    public void setAdditionalInformation(String information) {
        mInformationView.setText(information);
    }

    /**
     * Sets the duration text for the ListItem.
     */
    public void setDuration(String duration) {
        mDurationView.setText(duration);
    }
}
