package org.odyssey.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.odyssey.R;
import org.odyssey.views.FilesListViewItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FilesListViewAdapter extends BaseAdapter {

    private final Context mContext;
    private List<File> mFiles;

    public FilesListViewAdapter(Context context) {
        super();

        mContext = context;
        mFiles = new ArrayList<>();
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

    public void swapModel(List<File> files) {
        if (files == null) {
            mFiles.clear();
        } else {
            mFiles = files;
        }

        notifyDataSetChanged();
    }
}
