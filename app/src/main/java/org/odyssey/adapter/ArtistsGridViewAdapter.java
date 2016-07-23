package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.odyssey.models.ArtistModel;
import org.odyssey.views.ArtistsGridViewItem;

public class ArtistsGridViewAdapter extends GenericViewAdapter<ArtistModel> {

    private final GridView mRootGrid;
    private final Context mContext;

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

        // Check if a view can be recycled
        if (convertView != null) {
            ArtistsGridViewItem gridItem = (ArtistsGridViewItem) convertView;

            // Make sure to reset the layoutParams in case of change (rotation for example)
            ViewGroup.LayoutParams layoutParams = gridItem.getLayoutParams();
            layoutParams.height = mRootGrid.getColumnWidth();
            layoutParams.width = mRootGrid.getColumnWidth();
            gridItem.setLayoutParams(layoutParams);
            gridItem.setTitle(label);
            gridItem.setImageURL(imageURL);
        } else {
            // Create new view if no reusable is available
            convertView = new ArtistsGridViewItem(mContext, label, imageURL, new android.widget.AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        // Check if the scroll speed currently is already 0, then start the image task right away.
        if (mScrollSpeed == 0) {
            ((ArtistsGridViewItem) convertView).startCoverImageTask();
        }
        return convertView;
    }
}
