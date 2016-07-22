package org.odyssey.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.FileExplorerHelper;

import java.io.File;
import java.util.List;

public class FilesFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<File>>, AdapterView.OnItemClickListener {

    private FilesListViewAdapter mFilesListViewAdapter;
    private OnDirectorySelectedListener mOnDirectorySelectedCallback;

    private PlaybackServiceConnection mServiceConnection;

    private FileExplorerHelper mFileExplorerHelper;

    private File mCurrentDirectory;
    private boolean mIsRootDirectory = false;

    public final static String ARG_DIRECTORYPATH = "directory_path";
    public final static String ARG_ISROOTDIRECTORY = "is_root_directory";

    private final static String TAG = "OdysseyFilesFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_files, container, false);

        // get listview
        ListView filesListView = (ListView) rootView.findViewById(R.id.files_listview);

        // add progressbar
        filesListView.setEmptyView(rootView.findViewById(R.id.files_progressbar));

        mFilesListViewAdapter = new FilesListViewAdapter(getActivity());

        filesListView.setAdapter(mFilesListViewAdapter);
        filesListView.setOnItemClickListener(this);

        registerForContextMenu(filesListView);

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

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    @Override
    public Loader<List<File>> onCreateLoader(int arg0, Bundle bundle) {
        return new FileLoader(getActivity(), mCurrentDirectory, mFileExplorerHelper.getValidFileExtensions());
    }

    @Override
    public void onLoadFinished(Loader<List<File>> arg0, List<File> model) {
        mFilesListViewAdapter.swapModel(model);
    }

    @Override
    public void onLoaderReset(Loader<List<File>> arg0) {
        mFilesListViewAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File selectedFile = (File) mFilesListViewAdapter.getItem(position);

        // if file is directory open new fragment
        if (selectedFile.isDirectory()) {
            mOnDirectorySelectedCallback.onDirectorySelected(selectedFile.getPath(), false);
        }
    }

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

    private void enqueueFile(int position) {
        // Enqueue single file

        File currentFile = (File) mFilesListViewAdapter.getItem(position);

        // get a trackmodel for the current file
        TrackModel track = mFileExplorerHelper.getTrackModelForFile(currentFile);

        try {
            mServiceConnection.getPBS().enqueueTrack(track);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

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

    private void enqueueFolder(int position) {
        // Enqueue all music files in the current folder

        File currentFolder = (File) mFilesListViewAdapter.getItem(position);

        Log.v(TAG, "create trackmodel list");

        // get trackmodels for all music files in the folder
        List<TrackModel> tracks = mFileExplorerHelper.getTrackModelsForFolder(currentFolder);

        Log.v(TAG, "create trackmodel list done");

        for (TrackModel track : tracks) {
            try {
                // enqueue track
                mServiceConnection.getPBS().enqueueTrack(track);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
