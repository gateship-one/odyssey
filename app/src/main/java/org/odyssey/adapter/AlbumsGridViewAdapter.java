package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import org.odyssey.views.AlbumsGridViewItem;

import java.util.ArrayList;

public class AlbumsGridViewAdapter extends BaseAdapter{

    private GridView mRootGrid;
    private Context mContext;

    private ArrayList<String> mTitles;

    public AlbumsGridViewAdapter(Context context, GridView rootGrid) {
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
        mTitles.add("8");
        mTitles.add("9");
        mTitles.add("10");
        mTitles.add("11");
        mTitles.add("12");
        mTitles.add("13");
        mTitles.add("14");
        mTitles.add("15");
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
            AlbumsGridViewItem albumsGridViewItem = (AlbumsGridViewItem) convertView;
            albumsGridViewItem.setTitle(title);
        } else {
            convertView = new AlbumsGridViewItem(mContext, title, new AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        return convertView;
    }
}
