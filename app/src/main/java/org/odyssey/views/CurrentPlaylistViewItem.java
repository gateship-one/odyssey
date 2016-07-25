package org.odyssey.views;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import org.odyssey.R;
import org.odyssey.utils.ThemeUtils;

public class CurrentPlaylistViewItem extends GenericTracksListItem {

    private final TextView mSeparatorView;

    public CurrentPlaylistViewItem(Context context, String number, String title, String information, String duration) {
        super(context);

        mTitleView.setText(title);
        mNumberView.setText(number);
        mInformationView.setText(information);
        mDurationView.setText(duration);

        mSeparatorView = (TextView) findViewById(R.id.item_tracks_separator);
    }

    @Override
    TextView provideTitleView() {
        return (TextView) this.findViewById(R.id.item_tracks_title);
    }

    @Override
    TextView provideNumberView() {
        return (TextView) this.findViewById(R.id.item_tracks_number);
    }

    @Override
    TextView provideInformationView() {
        return (TextView) this.findViewById(R.id.item_tracks_additional_information);
    }

    @Override
    TextView provideDurationView() {
        return (TextView) this.findViewById(R.id.item_tracks_duration);
    }

    @Override
    int provideLayout() {
        return R.layout.listview_item_tracks;
    }

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
