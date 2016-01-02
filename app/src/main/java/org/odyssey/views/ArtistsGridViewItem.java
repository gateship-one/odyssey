package org.odyssey.views;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.odyssey.R;

public class ArtistsGridViewItem extends GenericGridItem{

    private TextView mTitleView;

    public ArtistsGridViewItem(Context context, String title, String imageURL, ViewGroup.LayoutParams layoutParams) {
        super(context,imageURL,layoutParams);

        mTitleView = ((TextView) this.findViewById(R.id.item_artists_title));
        mTitleView.setText(title);
    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    @Override
    ImageView provideImageView() {
        ImageView imageView = (ImageView) this.findViewById(R.id.item_artists_cover_image);
        return imageView;
    }

    @Override
    ViewSwitcher provideViewSwitcher() {
        ViewSwitcher viewSwitcher = (ViewSwitcher) this.findViewById(R.id.item_artists_view_switcher);
        return viewSwitcher;
    }

    @Override
    int provideLayout() {
        return R.layout.gridview_item_artists;
    }
}
