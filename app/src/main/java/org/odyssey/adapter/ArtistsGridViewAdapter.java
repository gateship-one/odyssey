package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import org.odyssey.models.ArtistModel;
import org.odyssey.views.ArtistsGridViewItem;

import java.util.ArrayList;

public class ArtistsGridViewAdapter extends BaseAdapter{

    private GridView mRootGrid;
    private Context mContext;

    private ArrayList<ArtistModel> mArtists;

    public ArtistsGridViewAdapter(Context context, GridView rootGrid) {
        super();

        mContext = context;
        mRootGrid = rootGrid;

        mArtists = new ArrayList<>();

        createDummyData(21);
    }

    private void createDummyData(int numberOfElements) {
        for(int i = 0; i < numberOfElements; i++) {
            ArtistModel album = new ArtistModel(""+i, "", "", -1);

            mArtists.add(album);
        }
    }

    @Override
    public int getCount() {

        return mArtists.size();
    }

    @Override
    public Object getItem(int position) {

        return mArtists.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ArtistModel artist = mArtists.get(position);

        // title
        String artistTitle = artist.getArtistName();

        if(convertView != null) {
            ArtistsGridViewItem artistsGridViewItem = (ArtistsGridViewItem) convertView;
            artistsGridViewItem.setTitle(artistTitle);
        } else {
            convertView = new ArtistsGridViewItem(mContext, artistTitle, new AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        return convertView;
    }
}
