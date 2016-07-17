package org.odyssey.fragments;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.BookmarksListViewAdapter;
import org.odyssey.loaders.BookmarkLoader;
import org.odyssey.models.BookmarkModel;

import java.util.List;

public class BookmarksFragment extends OdysseyFragment implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<List<BookmarkModel>> {
    private BookmarksListViewAdapter mBookmarksListViewAdapter;
    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_bookmarks, container, false);

        // get listview
        mListView = (ListView) rootView.findViewById(R.id.bookmarks_listview);

        mBookmarksListViewAdapter = new BookmarksListViewAdapter(getActivity());

        mListView.setAdapter(mBookmarksListViewAdapter);

        mListView.setOnItemClickListener(this);

        registerForContextMenu(mListView);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_bookmarks), false, true, false);

        activity.setUpPlayButton(null);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_bookmarks), false, true, false);

        activity.setUpPlayButton(null);

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }


    @Override
    public Loader<List<BookmarkModel>> onCreateLoader(int id, Bundle args) {
        return new BookmarkLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<BookmarkModel>> loader, List<BookmarkModel> data) {
        mBookmarksListViewAdapter.swapModel(data);
    }

    @Override
    public void onLoaderReset(Loader<List<BookmarkModel>> loader) {
        mBookmarksListViewAdapter.swapModel(null);
    }

    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
