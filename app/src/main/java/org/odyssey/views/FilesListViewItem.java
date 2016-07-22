package org.odyssey.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.odyssey.R;

public class FilesListViewItem extends RelativeLayout {

    TextView mTitleView;
    TextView mLastModifiedDateView;
    ImageView mIconView;

    public FilesListViewItem(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_files, this, true);

        mTitleView = (TextView) findViewById(R.id.item_files_title);

        mLastModifiedDateView = (TextView) findViewById(R.id.item_files_lastModifiedDate);

        mIconView = (ImageView) findViewById(R.id.item_files_icon);
    }

    public FilesListViewItem(Context context, String title, String lastModifiedDate, Drawable icon) {
        this(context);

        mTitleView.setText(title);
        mLastModifiedDateView.setText(lastModifiedDate);
        mIconView.setImageDrawable(icon);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    public void setModifiedDate(String dateString) {
        mLastModifiedDateView.setText(dateString);
    }

    public void setIcon(Drawable icon) {
        mIconView.setImageDrawable(icon);
    }
}
