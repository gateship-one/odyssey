/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.FilesAdapter;
import org.gateshipone.odyssey.dialogs.ChooseStorageVolumeDialog;
import org.gateshipone.odyssey.listener.OnDirectorySelectedListener;
import org.gateshipone.odyssey.loaders.FileLoader;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.utils.ThemeUtils;

import java.util.List;

public class FilesFragment extends OdysseyFragment<FileModel> implements AdapterView.OnItemClickListener {

    /**
     * Listener to open a child directory
     */
    private OnDirectorySelectedListener mOnDirectorySelectedCallback;

    /**
     * the current directory that is displayed by the fragment
     */
    private FileModel mCurrentDirectory;
    /**
     * flag if the current directory is a root directory
     */
    private boolean mIsRootDirectory = false;

    /**
     * key values for arguments of the fragment
     */
    public final static String ARG_DIRECTORYPATH = "directory_path";
    public final static String ARG_ISROOTDIRECTORY = "is_root_directory";

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_refresh, container, false);

        // get listview
        ListView filesListView = (ListView) rootView.findViewById(R.id.list_refresh_listview);

        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.list_refresh_swipe_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        mAdapter = new FilesAdapter(getActivity());

        filesListView.setAdapter(mAdapter);
        filesListView.setOnItemClickListener(this);

        // register listview for a context menu
        registerForContextMenu(filesListView);

        // activate options menu in toolbar
        setHasOptionsMenu(true);

        // get the current directory
        Bundle args = getArguments();

        if (args != null) {
            String directoryPath = args.getString(ARG_DIRECTORYPATH);
            mIsRootDirectory = args.getBoolean(ARG_ISROOTDIRECTORY);

            if (directoryPath != null) {
                mCurrentDirectory = new FileModel(directoryPath);
            }
        }

        return rootView;
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mOnDirectorySelectedCallback = (OnDirectorySelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnDirectorySelectedListener");
        }
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
            if (mIsRootDirectory) {
                mToolbarAndFABCallback.setupToolbar(mCurrentDirectory.getName(), false, true, false);
            } else {
                mToolbarAndFABCallback.setupToolbar(mCurrentDirectory.getName(), false, false, false);
            }
            // set up play button
            mToolbarAndFABCallback.setupFAB(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playCurrentFolderAndSubFolders();
                }
            });
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
    public Loader<List<FileModel>> onCreateLoader(int id, Bundle bundle) {
        return new FileLoader(getActivity(), mCurrentDirectory);
    }

    /**
     * Callback when an item in the ListView was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileModel selectedFile = (FileModel) mAdapter.getItem(position);

        if (selectedFile.isDirectory()) {
            // file is directory open new fragment
            mOnDirectorySelectedCallback.onDirectorySelected(selectedFile.getPath(), false);
        } else {
            // play the folder from the current position
            playFolder(position);
        }
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        FileModel currentFile = (FileModel) mAdapter.getItem(info.position);

        if (currentFile.isFile()) {
            // show context menu for files
            inflater.inflate(R.menu.context_menu_files_files_fragment, menu);
        } else {
            // show context menu for directories
            inflater.inflate(R.menu.context_menu_directories_files_fragment, menu);
        }
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
            case R.id.fragment_files_action_add_folder:
                enqueueFolderAndSubFolders(info.position);
                return true;
            case R.id.fragment_files_action_play_folder:
                playFolderAndSubFolders(info.position);
                return true;
            case R.id.fragment_files_action_add_file:
                enqueueFile(info.position, false);
                return true;
            case R.id.fragment_files_action_enqueueasnext:
                enqueueFile(info.position, true);
                return true;
            case R.id.fragment_files_action_play_file:
                playFile(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_files_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_add_directory).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_directory).setIcon(drawable);

        drawable = menu.findItem(R.id.action_switch_storage_volume).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_switch_storage_volume).setIcon(drawable);

        drawable = menu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_search).setIcon(drawable);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(new SearchTextObserver());

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_directory:
                enqueueCurrentFolderAndSubFolders();
                return true;
            case R.id.action_switch_storage_volume:
                ChooseStorageVolumeDialog chooseDialog = new ChooseStorageVolumeDialog();
                chooseDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChooseVolumeDialog");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Call the PBS to play the selected file.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected file
     */
    private void playFile(int position) {

        // clear playlist and play selected file

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueFile(position, false);
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the selected file.
     *
     * @param position the position of the selected file
     * @param asNext   flag if the file should be enqueued as next
     */
    private void enqueueFile(int position, boolean asNext) {

        FileModel currentFile = (FileModel) mAdapter.getItem(position);

        try {
            mServiceConnection.getPBS().enqueueFile(currentFile.getPath(), asNext);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play all music files from the selected folder (excluding subfolders) and starts with the selected file.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected file in the adapter
     */
    private void playFolder(int position) {
        try {
            mServiceConnection.getPBS().clearPlaylist();
            mServiceConnection.getPBS().enqueueDirectory(mCurrentDirectory.getPath());

            // compute position
            int index = position - mCurrentDirectory.getNumberOfSubFolders();
            mServiceConnection.getPBS().jumpTo(index);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Call the PBS to play all music files from the selected folder and his children.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected folder
     */
    private void playFolderAndSubFolders(int position) {

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueFolderAndSubFolders(position);
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue all music files from the selected folder and his children.
     *
     * @param position the position of the selected folder
     */
    private void enqueueFolderAndSubFolders(int position) {

        FileModel currentFolder = (FileModel) mAdapter.getItem(position);

        try {
            mServiceConnection.getPBS().enqueueDirectoryAndSubDirectories(currentFolder.getPath());
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Call the PBS to play all music in the current folder and his children.
     * A previous playlist will be cleared.
     */
    private void playCurrentFolderAndSubFolders() {

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueCurrentFolderAndSubFolders();
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue all music in the current folder and his children.
     */
    private void enqueueCurrentFolderAndSubFolders() {

        try {
            mServiceConnection.getPBS().enqueueDirectoryAndSubDirectories(mCurrentDirectory.getPath());
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Observer class to apply a filter
     */
    private class SearchTextObserver implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            if (query.isEmpty()) {
                removeFilter();
            } else {
                applyFilter(query);
            }

            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (newText.isEmpty()) {
                removeFilter();
            } else {
                applyFilter(newText);
            }

            return true;
        }
    }
}
