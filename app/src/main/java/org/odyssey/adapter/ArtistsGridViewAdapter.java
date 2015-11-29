package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import org.odyssey.views.ArtistsGridViewItem;

import java.util.ArrayList;

public class ArtistsGridViewAdapter extends BaseAdapter{

    private GridView mRootGrid;
    private Context mContext;

    private ArrayList<String> mTitles;

    public ArtistsGridViewAdapter(Context context, GridView rootGrid) {
        super();

        mContext = context;
        mRootGrid = rootGrid;

        mTitles = new ArrayList<>();
        mTitles.add("1");
        mTitles.add("2");
        mTitles.add("3");
        mTitles.add("4");
        mTitles.add("5");
        mTitles.add("6");
        mTitles.add("7");
    }

    @Override
    public int getCount() {
        return mTitles.size();
    }

    @Override
    public Object getItem(int position) {
        return mTitles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        String title = mTitles.get(position);

        if(convertView != null) {
            ArtistsGridViewItem artistsGridViewItem = (ArtistsGridViewItem) convertView;
            artistsGridViewItem.setTitle(title);
        } else {
            convertView = new ArtistsGridViewItem(mContext, title, new AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        return convertView;
    }
}
