package org.odyssey.views;

import android.content.Context;
import android.widget.TextView;

import org.odyssey.R;

public class CurrentPlaylistViewItem extends GenericListItem {

    TextView mSeparatorView;

    public CurrentPlaylistViewItem(Context context, String number, String title, String information, String duration) {
        super(context);

        mTitleView.setText(title);
        mNumberView.setText(number);
        mInformationView.setText(information);
        mDurationView.setText(duration);

        mSeparatorView = (TextView) findViewById(R.id.item_current_playlist_separator);
    }

    @Override
    TextView provideTitleView() {
        return (TextView) this.findViewById(R.id.item_current_playlist_title);
    }

    @Override
    TextView provideNumberView() {
        return (TextView) this.findViewById(R.id.item_current_playlist_number);
    }

    @Override
    TextView provideInformationView() {
        return (TextView) this.findViewById(R.id.item_current_playlist_additional_information);
    }

    @Override
    TextView provideDurationView() {
        return (TextView) this.findViewById(R.id.item_current_playlist_duration);
    }

    @Override
    int provideLayout() {
        return R.layout.listview_item_current_playlist;
    }

    public void setPlaying(boolean state) {
        if(state) {
            mTitleView.setTextColor(getResources().getColor(R.color.colorAccent));
            mNumberView.setTextColor(getResources().getColor(R.color.colorAccent));
            mSeparatorView.setTextColor(getResources().getColor(R.color.colorAccent));
        } else {
            mTitleView.setTextColor(getResources().getColor(R.color.colorTextLight));
            mNumberView.setTextColor(getResources().getColor(R.color.colorTextLight));
            mSeparatorView.setTextColor(getResources().getColor(R.color.colorTextLight));
        }

    }
}
