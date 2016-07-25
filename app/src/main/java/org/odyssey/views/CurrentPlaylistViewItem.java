package org.odyssey.views;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import org.odyssey.R;
import org.odyssey.utils.ThemeUtils;

public class CurrentPlaylistViewItem extends TracksListViewItem {

    private final TextView mSeparatorView;

    /**
     * Constructor that only initialize the layout.
     */
    public CurrentPlaylistViewItem(Context context) {
        super(context);

        mSeparatorView = (TextView) findViewById(R.id.item_tracks_separator);
    }

    /**
     * Constructor that already sets the values for each view.
     */
    public CurrentPlaylistViewItem(Context context, String number, String title, String information, String duration) {
        this(context);

        mTitleView.setText(title);
        mNumberView.setText(number);
        mInformationView.setText(information);
        mDurationView.setText(duration);
    }

    /**
     * Method that tint the title, number and separator view according to the state.
     * @param state flag indicates if the representing track is currently marked as played by the playbackservice
     */
    public void setPlaying(boolean state) {
        if(state) {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
            mTitleView.setTextColor(color);
            mNumberView.setTextColor(color);
            mSeparatorView.setTextColor(color);
        } else {
            int color = ContextCompat.getColor(getContext(), R.color.colorTextLight);
            mTitleView.setTextColor(color);
            mNumberView.setTextColor(color);
            mSeparatorView.setTextColor(color);
        }

    }
}
