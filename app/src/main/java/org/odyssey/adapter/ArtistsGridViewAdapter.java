package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.odyssey.models.ArtistModel;
import org.odyssey.views.ArtistsGridViewItem;

public class ArtistsGridViewAdapter extends GenericViewAdapter<ArtistModel> {

    private GridView mRootGrid;

    private Context mContext;

    public ArtistsGridViewAdapter(Context context, GridView rootGrid) {
        super();

        mContext = context;
        mRootGrid = rootGrid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ArtistModel artist = mModelData.get(position);
        String label = artist.getArtistName();
        String imageURL = artist.getArtistURL();

        if (convertView != null) {
            ArtistsGridViewItem gridItem = (ArtistsGridViewItem) convertView;
            gridItem.setTitle(label);
            gridItem.setImageURL(imageURL);
        } else {
            convertView = new ArtistsGridViewItem(mContext, label, imageURL, new android.widget.AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        if (mScrollSpeed == 0) {
            ((ArtistsGridViewItem) convertView).startCoverImageTask();
        }
        return convertView;
    }
}
