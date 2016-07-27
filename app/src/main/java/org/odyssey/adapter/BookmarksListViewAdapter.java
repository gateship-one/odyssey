package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.R;
import org.odyssey.models.BookmarkModel;
import org.odyssey.utils.FormatHelper;
import org.odyssey.views.BookmarksListViewItem;

public class BookmarksListViewAdapter extends GenericViewAdapter<BookmarkModel> {

    private final Context mContext;

    public BookmarksListViewAdapter(Context context) {
        super();

        mContext = context;
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
        BookmarkModel bookmark = mModelData.get(position);

        // title
        String bookmarkTitle = bookmark.getTitle();

        // number of tracks
        String numberOfTracks = Integer.toString(bookmark.getNumberOfTracks()) + " " + mContext.getString(R.string.fragment_bookmarks_tracks);

        // get date string
        String dateString = FormatHelper.formatTimeStampToString(bookmark.getId());

        if (convertView != null) {
            BookmarksListViewItem bookmarksListViewItem = (BookmarksListViewItem) convertView;

            bookmarksListViewItem.setTitle(bookmarkTitle);
            bookmarksListViewItem.setNumberOfTracks(numberOfTracks);
            bookmarksListViewItem.setDate(dateString);
        } else {
            convertView = new BookmarksListViewItem(mContext, bookmarkTitle, numberOfTracks, dateString);
        }

        return convertView;
    }
}
