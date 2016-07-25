package org.odyssey.adapter;

import org.odyssey.models.AlbumModel;
import org.odyssey.views.GridViewItem;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

public class AlbumsGridViewAdapter extends GenericViewAdapter<AlbumModel> {

    private final Context mContext;

    /**
     * The parent grid to adjust the layoutparams.
     */
    private final GridView mRootGrid;

    public AlbumsGridViewAdapter(Context context, GridView rootGrid) {
        super();

        mContext = context;
        mRootGrid = rootGrid;
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     * @param position The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AlbumModel album = mModelData.get(position);
        String label = album.getAlbumName();
        String imageURL = album.getAlbumArtURL();

        if (convertView != null) {
            GridViewItem gridItem = (GridViewItem) convertView;

            // Make sure to reset the layoutParams in case of change (rotation for example)
            ViewGroup.LayoutParams layoutParams = gridItem.getLayoutParams();
            layoutParams.height = mRootGrid.getColumnWidth();
            layoutParams.width = mRootGrid.getColumnWidth();
            gridItem.setLayoutParams(layoutParams);

            gridItem.setTitle(label);
            gridItem.setImageURL(imageURL);
        } else {
            convertView = new GridViewItem(mContext, label, imageURL, new android.widget.AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        // Check if the scroll speed currently is already 0, then start the image task right away.
        if (mScrollSpeed == 0) {
            ((GridViewItem) convertView).startCoverImageTask();
        }
        return convertView;
    }
}