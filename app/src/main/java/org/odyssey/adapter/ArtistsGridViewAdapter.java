package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.SectionIndexer;

import org.odyssey.models.ArtistModel;
import org.odyssey.views.ArtistsGridViewItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArtistsGridViewAdapter extends BaseAdapter implements SectionIndexer, ScrollSpeedAdapter{

    private GridView mRootGrid;

    ArrayList<String> mSectionList;
    ArrayList<Integer> mSectionPositions;
    HashMap<Character, Integer> mPositionSectionMap;
    private Context mContext;

    private List<ArtistModel> mArtists;

    private int mScrollSpeed;

    public ArtistsGridViewAdapter(Context context, GridView rootGrid) {
        super();

        mSectionList = new ArrayList<String>();
        mSectionPositions = new ArrayList<Integer>();
        mPositionSectionMap = new HashMap<Character, Integer>();

        mContext = context;
        mRootGrid = rootGrid;

        mArtists = new ArrayList<>();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sectionIndex >= 0 && sectionIndex < mSectionPositions.size()) {
            return mSectionPositions.get(sectionIndex);
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int pos) {

        ArtistModel artist = (ArtistModel) getItem(pos);

        String artistsName = artist.getArtistName();

        char artistSection = artistsName.toUpperCase().charAt(0);

        if (mPositionSectionMap.containsKey(artistSection)) {
            int sectionIndex = mPositionSectionMap.get(artistSection);
            return sectionIndex;
        }

        return 0;
    }

    @Override
    public Object[] getSections() {
        return mSectionList.toArray();
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

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to call.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param artists
     *            Actual model data
     */
    public void swapModel(List<ArtistModel> artists) {
        if (artists == null) {
            mArtists.clear();
        } else {
            mArtists = artists;
        }
        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mArtists.size() > 0) {
            char lastSection = 0;

            ArtistModel currentArtist = mArtists.get(0);

            lastSection = currentArtist.getArtistName().toUpperCase().charAt(0);

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentArtist = mArtists.get(i);

                char currentSection = currentArtist.getArtistName().toUpperCase().charAt(0);

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ArtistModel artist = mArtists.get(position);
        String label = artist.getArtistName();
        String imageURL = artist.getArtURL();

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

    /**
     * Sets the scrollspeed of the parent GridView. For smoother scrolling
     *
     * @param speed
     */
    public void setScrollSpeed(int speed) {
        mScrollSpeed = speed;
    }

}
