package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.odyssey.R;

public class BookmarksListViewItem extends LinearLayout {

    TextView mTitleView;
    TextView mNumberOfTracksView;
    TextView mDateStringView;

    public BookmarksListViewItem(Context context, String title, String numberOfTracks, String dateString) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_bookmarks, this, true);

        mTitleView = (TextView) findViewById(R.id.item_bookmarks_title);
        mTitleView.setText(title);

        mNumberOfTracksView = (TextView) findViewById(R.id.item_bookmarks_numberOfTracks);
        mNumberOfTracksView.setText(numberOfTracks);

        mDateStringView = (TextView) findViewById(R.id.item_bookmarks_date);
        mDateStringView.setText(dateString);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    public void setNumberOfTracks(String numberOfTracks) {
        mNumberOfTracksView.setText(numberOfTracks);
    }

    public void setDate(String dateString) {
        mDateStringView.setText(dateString);
    }
}
