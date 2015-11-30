package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.odyssey.R;

public class AlbumsGridViewItem extends RelativeLayout{

    private TextView mTitleView;

    public AlbumsGridViewItem(Context context, String title, ViewGroup.LayoutParams layoutParams) {
        super(context);

        // TODO check this
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gridview_item_albums, this, true);

        setLayoutParams(layoutParams);

        mTitleView = (TextView) this.findViewById(R.id.item_albums_title);
        mTitleView.setText(title);

    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }
}
