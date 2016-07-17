package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.R;
import org.odyssey.models.BookmarkModel;
import org.odyssey.views.BookmarksListViewItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookmarksListViewAdapter extends GenericViewAdapter<BookmarkModel> {

    private final Context mContext;

    public BookmarksListViewAdapter(Context context) {
        super();

        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BookmarkModel bookmark = mModelData.get(position);

        // title
        String bookmarkTitle = bookmark.getTitle();

        // number of tracks
        String numberOfTracks = Integer.toString(bookmark.getNumberOfTracks()) + " " + mContext.getString(R.string.fragment_bookmarks_tracks);

        // get formatted date string
        long timestamp = bookmark.getId();

        Date date = new Date(timestamp);
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);

        // autosave flag
        String autosave = (bookmark.getAutoSave() == 1) ? mContext.getString(R.string.fragment_bookmarks_autosave_yes) : mContext.getString(R.string.fragment_bookmarks_autosave_no);

        if (convertView != null) {
            BookmarksListViewItem bookmarksListViewItem = (BookmarksListViewItem) convertView;

            bookmarksListViewItem.setTitle(bookmarkTitle);
            bookmarksListViewItem.setNumberOfTracks(numberOfTracks);
            bookmarksListViewItem.setDate(dateString);
            bookmarksListViewItem.setAutosave(autosave);
        } else {
            convertView = new BookmarksListViewItem(mContext, bookmarkTitle, numberOfTracks, dateString, autosave);
        }

        return convertView;
    }
}
