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

    private final Context mContext;
    private List<File> mFiles;
    private final ArrayList<String> mSectionList;
    private final ArrayList<Integer> mSectionPositions;
    private final HashMap<Character, Integer> mPositionSectionMap;

    public FilesListViewAdapter(Context context) {
        super();

        mContext = context;
        mFiles = new ArrayList<>();
        mSectionList = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        mPositionSectionMap = new HashMap<>();
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        File file = mFiles.get(position);

        // title
        String title = file.getName();

        // get icon for filetype
        Drawable icon;
        if (file.isDirectory()) {
            // choose directory icon
            icon = mContext.getDrawable(R.drawable.ic_folder_24dp);
        } else {
            // choose file icon
            icon = mContext.getDrawable(R.drawable.ic_file_24dp);
        }

        if (icon != null) {
            // tint the icon
            DrawableCompat.setTint(icon, ContextCompat.getColor(mContext, R.color.colorTextLight));
        }

        // last modified
        long lastModified = file.lastModified();

        Date date = new Date(lastModified);
        String lastModifiedDateString = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);

        if(convertView != null) {
            FilesListViewItem filesListViewItem = (FilesListViewItem) convertView;
            filesListViewItem.setTitle(title);
            filesListViewItem.setModifiedDate(lastModifiedDateString);
            filesListViewItem.setIcon(icon);
        } else {
            convertView = new FilesListViewItem(mContext, title, lastModifiedDateString, icon);
        }

        return convertView;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int pos) {

        String sectionTitle = mFiles.get(pos).getName();

        char fileSection = sectionTitle.toUpperCase().charAt(0);

        if (mPositionSectionMap.containsKey(fileSection)) {
            int sectionIndex = mPositionSectionMap.get(fileSection);
            return sectionIndex;
        }
        return 0;
    }

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
     * @param files
     *            Actual filelist
     */
    public void swapModel(List<File> files) {
        if (files == null) {
            mFiles.clear();
        } else {
            mFiles = files;
        }

        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mFiles.size() > 0) {
            File currentFile = mFiles.get(0);

            char lastSection = currentFile.getName().toUpperCase().charAt(0);

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentFile = mFiles.get(i);

                char currentSection = currentFile.getName().toUpperCase().charAt(0);

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
        notifyDataSetChanged();

        notifyDataSetChanged();
    }
}
