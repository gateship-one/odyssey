package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.odyssey.R;

public class AlbumsGridViewItem extends RelativeLayout{

    private TextView mTitleView;

    public AlbumsGridViewItem(Context context, String title, ViewGroup.LayoutParams layoutParams) {
        super(context);

        View rootView = LayoutInflater.from(context).inflate(R.layout.gridview_item_albums, this, true);

        setLayoutParams(layoutParams);

        mTitleView = (TextView) rootView.findViewById(R.id.item_albums_title);
        mTitleView.setText(title);

    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }
}
