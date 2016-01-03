package org.odyssey.adapter;

import org.odyssey.models.AlbumModel;
import org.odyssey.views.AlbumsGridViewItem;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

public class AlbumsGridViewAdapter extends GenericViewAdapter<AlbumModel> {

    private Context mContext;

    private GridView mRootGrid;

    public AlbumsGridViewAdapter(Context context, GridView rootGrid) {
        super();

        mContext = context;

        mRootGrid = rootGrid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AlbumModel album = mModelData.get(position);
        String label = album.getAlbumName();
        String imageURL = album.getAlbumArtURL();

        if (convertView != null) {
            AlbumsGridViewItem gridItem = (AlbumsGridViewItem) convertView;
            gridItem.setTitle(label);
            gridItem.setImageURL(imageURL);
        } else {
            convertView = new AlbumsGridViewItem(mContext, label, imageURL, new android.widget.AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        if (mScrollSpeed == 0) {
            ((AlbumsGridViewItem) convertView).startCoverImageTask();
        }
        return convertView;
    }
}