/*
 * Copyright (C) 2018 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.fragments;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.BookmarksAdapter;
import org.gateshipone.odyssey.loaders.BookmarkLoader;
import org.gateshipone.odyssey.models.BookmarkModel;

import java.util.List;

public class BookmarksFragment extends OdysseyFragment<BookmarkModel> implements AdapterView.OnItemClickListener {

    public static BookmarksFragment newInstance() {
        return new BookmarksFragment();
    }

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_linear, container, false);

        // get listview
        mListView = rootView.findViewById(R.id.list_linear_listview);

        mAdapter = new BookmarksAdapter(getActivity());

        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) rootView.findViewById(R.id.empty_view_message)).setText(R.string.empty_bookmarks_message);

        registerForContextMenu(mListView);

        return rootView;
    }

    /**
     * Called when the fragment resumes.
     * Reload the data, setup the toolbar and create the PBS connection.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_bookmarks), false, true, false);
            // set up play button
            mToolbarAndFABCallback.setupFAB(null);
        }
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<BookmarkModel>> onCreateLoader(int id, Bundle bundle) {
        return new BookmarkLoader(getActivity(), false);
    }

    /**
     * Play the clicked bookmark.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        resumeBookmark(position);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_bookmarks_fragment, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.bookmarks_context_menu_action_resume:
                resumeBookmark(info.position);
                return true;
            case R.id.bookmarks_context_menu_action_delete:
                deleteBookmark(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Call the PBS to play the selected bookmark.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected bookmark in the adapter
     */
    private void resumeBookmark(int position) {
        // identify current bookmark
        BookmarkModel bookmark = mAdapter.getItem(position);

        // resume state
        try {
            mServiceConnection.getPBS().resumeBookmark(bookmark.getId());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to delete the selected bookmark.
     *
     * @param position the position of the selected bookmark in the adapter
     */
    private void deleteBookmark(int position) {
        // identify current bookmark
        BookmarkModel bookmark = mAdapter.getItem(position);

        // delete state
        try {
            mServiceConnection.getPBS().deleteBookmark(bookmark.getId());

            refreshContent();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
