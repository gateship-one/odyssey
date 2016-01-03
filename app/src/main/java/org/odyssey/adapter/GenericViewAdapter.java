package org.odyssey.adapter;

import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import org.odyssey.models.GenericModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class GenericViewAdapter extends BaseAdapter implements SectionIndexer, ScrollSpeedAdapter {

    ArrayList<String> mSectionList;
    ArrayList<Integer> mSectionPositions;
    HashMap<Character, Integer> mPositionSectionMap;

    protected List<GenericModel> mModelData;
    protected int mScrollSpeed;

    public GenericViewAdapter() {
        super();

        mSectionList = new ArrayList<String>();
        mSectionPositions = new ArrayList<Integer>();
        mPositionSectionMap = new HashMap<Character, Integer>();

        mModelData = new ArrayList<GenericModel>();
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to jll.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param data
     *            Actual model data
     */
    public void swapModel(List<GenericModel> data) {
        if (data == null) {
            mModelData.clear();
        } else {
            mModelData = data;
        }
        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mModelData.size() > 0) {
            char lastSection = 0;

            GenericModel currentModel = mModelData.get(0);

            lastSection = currentModel.getSectionTitle().toUpperCase().charAt(0);

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentModel = mModelData.get(i);

                char currentSection = currentModel.getSectionTitle().toUpperCase().charAt(0);

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
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int pos) {

        String sectionTitle = mModelData.get(pos).getSectionTitle();

        char albumSection = sectionTitle.toUpperCase().charAt(0);

        if (mPositionSectionMap.containsKey(albumSection)) {
            int sectionIndex = mPositionSectionMap.get(albumSection);
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
        return mModelData.size();
    }

    @Override
    public Object getItem(int position) {
        return mModelData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
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
