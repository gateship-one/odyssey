package org.odyssey.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import org.odyssey.R;
import org.odyssey.views.FilesListViewItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class FilesListViewAdapter extends BaseAdapter implements SectionIndexer {

    /**
     * Variables used for sectioning (fast scroll).
     */
    private final ArrayList<String> mSectionList;
    private final ArrayList<Integer> mSectionPositions;
    private final HashMap<Character, Integer> mPositionSectionMap;

    private final Context mContext;

    /**
     * List with model data used for this adapter.
     */
    private List<File> mModelData;

    public FilesListViewAdapter(Context context) {
        super();

        mContext = context;

        mModelData = new ArrayList<>();

        mSectionList = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        mPositionSectionMap = new HashMap<>();
    }

    /**
     * @return The length of the model data of this adapter.
     */
    @Override
    public int getCount() {
        return mModelData.size();
    }

    /**
     * Simple getter for the model data.
     *
     * @param position Index of the item to get. No check for boundaries here.
     * @return The item at index position.
     */
    @Override
    public Object getItem(int position) {
        return mModelData.get(position);
    }

    /**
     * Simple position->id mapping here.
     *
     * @param position Position to get the id from
     * @return The id (position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     *
     * @param position    The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent      The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        File file = mModelData.get(position);

        // title
        String title = file.getName();

        // get icon for filetype
        Drawable icon;
        if (file.isDirectory()) {
            // choose directory icon
            icon = mContext.getDrawable(R.drawable.ic_folder_48dp);
        } else {
            // choose file icon
            icon = mContext.getDrawable(R.drawable.ic_file_48dp);
        }

        if (icon != null) {
            // tint the icon
            DrawableCompat.setTint(icon, ContextCompat.getColor(mContext, R.color.colorTextLight));
        }

        // last modified
        long lastModified = file.lastModified();

        Date date = new Date(lastModified);
        String lastModifiedDateString = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);

        // Check if a view can be recycled
        if (convertView != null) {
            FilesListViewItem filesListViewItem = (FilesListViewItem) convertView;
            filesListViewItem.setTitle(title);
            filesListViewItem.setModifiedDate(lastModifiedDateString);
            filesListViewItem.setIcon(icon);
        } else {
            // Create new view if no reusable is available
            convertView = new FilesListViewItem(mContext, title, lastModifiedDateString, icon);
        }

        return convertView;
    }

    /**
     * Looks up the position(index) of a given section(index)
     *
     * @param sectionIndex Section to get the ListView position for
     * @return The item position of this section start.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    /**
     * Reverse lookup of a section for a given position
     *
     * @param pos Position to get the section for
     * @return Section (index) for the items position
     */
    @Override
    public int getSectionForPosition(int pos) {

        String sectionTitle = mModelData.get(pos).getName();

        char itemSection;
        if (sectionTitle.length() > 0) {
            itemSection = sectionTitle.toUpperCase().charAt(0);
        } else {
            itemSection = ' ';
        }

        if (mPositionSectionMap.containsKey(itemSection)) {
            return mPositionSectionMap.get(itemSection);
        }
        return 0;
    }

    /**
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        return mSectionList.toArray();
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the ListItems. This should generally be safe to call.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param files Actual filelist
     */
    public void swapModel(List<File> files) {
        if (files == null) {
            mModelData.clear();
        } else {
            mModelData = files;
        }

        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mModelData.size() > 0) {
            File currentFile = mModelData.get(0);

            char lastSection;
            if (currentFile.getName().length() > 0) {
                lastSection = currentFile.getName().toUpperCase().charAt(0);
            } else {
                lastSection = ' ';
            }

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentFile = mModelData.get(i);

                char currentSection;
                if (currentFile.getName().length() > 0) {
                    currentSection = currentFile.getName().toUpperCase().charAt(0);
                } else {
                    currentSection = ' ';
                }

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
        // dataset has changed so notify the related ListView to refresh
        notifyDataSetChanged();
    }
}
