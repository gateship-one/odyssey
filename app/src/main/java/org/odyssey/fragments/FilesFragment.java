package org.odyssey.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.FilesListViewAdapter;
import org.odyssey.listener.OnDirectorySelectedListener;
import org.odyssey.loaders.FileLoader;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.FileExplorerHelper;
import org.odyssey.utils.ThemeUtils;

import java.io.File;
import java.util.List;

public class FilesFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<File>>, AdapterView.OnItemClickListener {

    /**
     * Adapter used for the ListView
     */
    private FilesListViewAdapter mFilesListViewAdapter;

    /**
     * Listener to open a child directory
     */
    private OnDirectorySelectedListener mOnDirectorySelectedCallback;

    /**
     * ServiceConnection object to communicate with the PlaybackService
     */
    private PlaybackServiceConnection mServiceConnection;

    /**
     * Helper object for file operations
     */
    private FileExplorerHelper mFileExplorerHelper;

    /**
     * Save the swipe layout for later usage
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * the current directory that is displayed by the fragment
     */
    private File mCurrentDirectory;
    /**
     * flag if the current directory is a root directory
     */
    private boolean mIsRootDirectory = false;

    /**
     * key values for arguments of the fragment
     */
    public final static String ARG_DIRECTORYPATH = "directory_path";
    public final static String ARG_ISROOTDIRECTORY = "is_root_directory";

    private final static String TAG = "OdysseyFilesFragment";

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
                refresh();
            }
        });

        mFilesListViewAdapter = new FilesListViewAdapter(getActivity());

        filesListView.setAdapter(mFilesListViewAdapter);
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
                mCurrentDirectory = new File(directoryPath);
            }
        }

        // get fileexplorerhelper
        mFileExplorerHelper = FileExplorerHelper.getInstance(getContext());

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

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        if (mIsRootDirectory) {
            activity.setUpToolbar(mCurrentDirectory.getName(), false, true, false);
        } else {
            activity.setUpToolbar(mCurrentDirectory.getName(), false, false, false);
        }

        // set up play button
        activity.setUpPlayButton(null);

        // set up pbs connection
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

        // change refresh state
        mSwipeRefreshLayout.setRefreshing(true);
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    /**
     * generic method to reload the dataset displayed by the fragment
     */
    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<File>> onCreateLoader(int id, Bundle bundle) {
        return new FileLoader(getActivity(), mCurrentDirectory, mFileExplorerHelper.getValidFileExtensions());
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param model  Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<File>> loader, List<File> model) {
        mFilesListViewAdapter.swapModel(model);
        // change refresh state
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<File>> loader) {
        mFilesListViewAdapter.swapModel(null);
    }

    /**
     * Callback when an item in the ListView was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File selectedFile = (File) mFilesListViewAdapter.getItem(position);

        // if file is directory open new fragment
        if (selectedFile.isDirectory()) {
            mOnDirectorySelectedCallback.onDirectorySelected(selectedFile.getPath(), false);
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
        File currentFile = (File) mFilesListViewAdapter.getItem(info.position);

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
                enqueueFolder(info.position);
                return true;
            case R.id.fragment_files_action_play_folder:
                playFolder(info.position);
                return true;
            case R.id.fragment_files_action_add_file:
                enqueueFile(info.position);
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

        Drawable drawable = menu.findItem(R.id.action_switch_storage_volume).getIcon();

        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(getContext(), R.color.colorTextLight));
        menu.findItem(R.id.action_switch_storage_volume).setIcon(drawable);

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
            enqueueFile(position);
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
     */
    private void enqueueFile(int position) {
        // Enqueue single file

        File currentFile = (File) mFilesListViewAdapter.getItem(position);

        // get a trackmodel for the current file
        //TrackModel track = mFileExplorerHelper.getTrackModelForFile(currentFile);

        try {
            mServiceConnection.getPBS().enqueueFile(currentFile.toString());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play all music files from the selected folder and his children.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected folder
     */
    private void playFolder(int position) {
        // clear playlist and play all music files in the selected folder

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueFolder(position);
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
    private void enqueueFolder(int position) {
        // Enqueue all music files in the current folder

        File currentFolder = (File) mFilesListViewAdapter.getItem(position);

        try {
            mServiceConnection.getPBS().enqueueDirectory(currentFolder.toString());
            //enqueueFolder(position);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
}
