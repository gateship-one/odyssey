package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.odyssey.R;

public class ArtistsGridViewItem extends RelativeLayout{

    TextView mTitleView;

    public ArtistsGridViewItem(Context context, String title, ViewGroup.LayoutParams layoutParams) {
        super(context);
        // TODO check this
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gridview_item_artists, this, true);

        setLayoutParams(layoutParams);

        mTitleView = (TextView) this.findViewById(R.id.item_artists_title);
        mTitleView.setText(title);

    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }
}
