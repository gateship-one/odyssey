package org.odyssey.views;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.widget.TextView;

import org.odyssey.R;

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
            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.odyssey_color_accent,typedValue,true);
            mTitleView.setTextColor(typedValue.data);
            mNumberView.setTextColor(typedValue.data);
            mSeparatorView.setTextColor(typedValue.data);
        } else {
            mTitleView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextLight));
            mNumberView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextLight));
            mSeparatorView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextLight));
        }

    }
}
