package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import org.odyssey.models.AlbumModel;
import org.odyssey.views.AlbumsGridViewItem;

import java.util.ArrayList;

public class AlbumsGridViewAdapter extends BaseAdapter{

    private GridView mRootGrid;
    private Context mContext;

    private ArrayList<AlbumModel> mAlbums;

    public AlbumsGridViewAdapter(Context context, GridView rootGrid) {
        super();

        mContext = context;
        mRootGrid = rootGrid;

        mAlbums = new ArrayList<>();

        createDummyData(36);
    }

    private void createDummyData(int numberOfElements) {
        for(int i = 0; i < numberOfElements; i++) {
            AlbumModel album = new AlbumModel(""+i, "", "", "");

            mAlbums.add(album);
        }
    }

    @Override
    public int getCount() {

        return mAlbums.size();
    }

    @Override
    public Object getItem(int position) {

        return mAlbums.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        AlbumModel album = mAlbums.get(position);

        // title
        String albumTitle = album.getAlbumName();

        if(convertView != null) {
            AlbumsGridViewItem albumsGridViewItem = (AlbumsGridViewItem) convertView;
            albumsGridViewItem.setTitle(albumTitle);
        } else {
            convertView = new AlbumsGridViewItem(mContext, albumTitle, new AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        return convertView;
    }
}
