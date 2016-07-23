package org.odyssey.adapter;

import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import org.odyssey.models.GenericModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class GenericViewAdapter<T extends GenericModel> extends BaseAdapter implements SectionIndexer, ScrollSpeedAdapter {
    /**
     * Variables used for sectioning (fast scroll).
     */
    private final ArrayList<String> mSectionList;
    private final ArrayList<Integer> mSectionPositions;
    private final HashMap<Character, Integer> mPositionSectionMap;

    /**
     * Abstract list with model data used for this adapter.
     */
    protected List<T> mModelData;

    /**
     * Variable to store the current scroll speed. Used for image view optimizations
     */
    protected int mScrollSpeed;

    public GenericViewAdapter() {
        super();

        mSectionList = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        mPositionSectionMap = new HashMap<>();

        mModelData = new ArrayList<>();
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
    public void swapModel(List<T> data) {
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
            GenericModel currentModel = mModelData.get(0);

            char lastSection;
            if ( currentModel.getSectionTitle().length() > 0 ) {
                lastSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
            } else {
                lastSection = ' ';
            }

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

        char albumSection;
        if (sectionTitle.length() > 0) {
            albumSection = sectionTitle.toUpperCase().charAt(0);
        } else {
            albumSection = ' ';
        }

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
