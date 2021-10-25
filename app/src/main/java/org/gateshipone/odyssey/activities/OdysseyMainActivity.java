/*
 * Copyright (C) 2020 Team Gateship-One
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

package org.gateshipone.odyssey.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.transition.Slide;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.CurrentPlaylistAdapter;
import org.gateshipone.odyssey.dialogs.SaveDialog;
import org.gateshipone.odyssey.fragments.AlbumTracksFragment;
import org.gateshipone.odyssey.fragments.ArtistAlbumsFragment;
import org.gateshipone.odyssey.fragments.ArtworkSettingsFragment;
import org.gateshipone.odyssey.fragments.BookmarksFragment;
import org.gateshipone.odyssey.fragments.FilesFragment;
import org.gateshipone.odyssey.fragments.InformationSettingsFragment;
import org.gateshipone.odyssey.fragments.MyMusicFragment;
import org.gateshipone.odyssey.fragments.OdysseyFragment;
import org.gateshipone.odyssey.fragments.PlaylistTracksFragment;
import org.gateshipone.odyssey.fragments.RecentAlbumsFragment;
import org.gateshipone.odyssey.fragments.SavedPlaylistsFragment;
import org.gateshipone.odyssey.fragments.SettingsFragment;
import org.gateshipone.odyssey.listener.OnAlbumSelectedListener;
import org.gateshipone.odyssey.listener.OnArtistSelectedListener;
import org.gateshipone.odyssey.listener.OnDirectorySelectedListener;
import org.gateshipone.odyssey.listener.OnPlaylistSelectedListener;
import org.gateshipone.odyssey.listener.OnRecentAlbumsSelectedListener;
import org.gateshipone.odyssey.listener.OnSaveDialogListener;
import org.gateshipone.odyssey.listener.OnStartSleepTimerListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.utils.FileExplorerHelper;
import org.gateshipone.odyssey.utils.FileUtils;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.PermissionHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.SearchViewModel;
import org.gateshipone.odyssey.views.CurrentPlaylistView;
import org.gateshipone.odyssey.views.NowPlayingView;

import java.util.List;

public class OdysseyMainActivity extends GenericActivity
        implements NavigationView.OnNavigationItemSelectedListener, ToolbarAndFABCallback,
        OnSaveDialogListener, NowPlayingView.NowPlayingDragStatusReceiver, SettingsFragment.OnArtworkSettingsRequestedCallback,
        OnArtistSelectedListener, OnAlbumSelectedListener, OnRecentAlbumsSelectedListener,
        OnPlaylistSelectedListener, OnDirectorySelectedListener, OnStartSleepTimerListener {

    public enum REQUESTEDVIEW {
        NONE,
        NOWPLAYING,
        SETTINGS
    }

    private ActionBarDrawerToggle mDrawerToggle;

    private DRAG_STATUS mNowPlayingDragStatus;

    private DRAG_STATUS mSavedNowPlayingDragStatus = null;

    private VIEW_SWITCHER_STATUS mNowPlayingViewSwitcherStatus;

    private VIEW_SWITCHER_STATUS mSavedNowPlayingViewSwitcherStatus = null;

    private FileExplorerHelper mFileExplorerHelper = null;

    public final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW = "org.gateshipone.odyssey.requestedview";

    public final static String MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS = "OdysseyMainActivity.NowPlayingDragStatus";

    public final static String MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW = "OdysseyMainActivity.NowPlayingViewSwitcherCurrentView";

    private Uri mSentUri;

    private boolean mShowNPV = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean switchToSettings = false;

        // restore drag state
        if (savedInstanceState != null) {
            mSavedNowPlayingDragStatus = DRAG_STATUS.values()[savedInstanceState.getInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS)];
            mSavedNowPlayingViewSwitcherStatus = VIEW_SWITCHER_STATUS.values()[savedInstanceState.getInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW)];
        } else {
            // if no savedInstanceState is present the activity is started for the first time so check the intent
            final Intent intent = getIntent();
            if (intent != null) {
                if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
                    // odyssey was opened by a file so save the uri for later usage when the service is running
                    mSentUri = intent.getData();
                } else {
                    // odyssey was opened by widget or notification
                    final Bundle extras = intent.getExtras();

                    if (extras != null) {
                        REQUESTEDVIEW requestedView = REQUESTEDVIEW.values()[extras.getInt(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, REQUESTEDVIEW.NONE.ordinal())];

                        switch (requestedView) {
                            case NONE:
                                break;
                            case NOWPLAYING:
                                mShowNPV = true;
                                break;
                            case SETTINGS:
                                switchToSettings = true;
                                break;
                        }
                    }
                }
            }
        }

        // Get preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_odyssey_main);

        // restore elevation behaviour as pre 24 support lib
        AppBarLayout layout = findViewById(R.id.appbar);
        layout.setStateListAnimator(null);
        ViewCompat.setElevation(layout, 0);

        // get fileexplorerhelper
        mFileExplorerHelper = FileExplorerHelper.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // enable back navigation
        final androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        }

        int navId = switchToSettings ? R.id.nav_settings : getDefaultViewID();

        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.setCheckedItem(navId);
        }

        // register context menu for currentPlaylistListView
        ListView currentPlaylistListView = findViewById(R.id.list_linear_listview);
        registerForContextMenu(currentPlaylistListView);

        if (findViewById(R.id.fragment_container) != null && (savedInstanceState == null)) {
            Fragment fragment;

            if (navId == R.id.nav_saved_playlists) {
                fragment = SavedPlaylistsFragment.newInstance();
            } else if (navId == R.id.nav_bookmarks) {
                fragment = BookmarksFragment.newInstance();

            } else if (navId == R.id.nav_files) {
                // open the default directory
                List<String> storageVolumesList = mFileExplorerHelper.getStorageVolumes(getApplicationContext());

                String defaultDirectory = "/";

                if (!storageVolumesList.isEmpty()) {
                    // choose the latest used storage volume as default
                    defaultDirectory = sharedPref.getString(getString(R.string.pref_file_browser_root_dir_key), storageVolumesList.get(0));
                }

                fragment = FilesFragment.newInstance(defaultDirectory, storageVolumesList.contains(defaultDirectory));
            } else if (navId == R.id.nav_settings) {
                fragment = SettingsFragment.newInstance();
            } else if (navId == R.id.nav_my_music) {
                fragment = MyMusicFragment.newInstance(getDefaultTab());
            } else {
                fragment = MyMusicFragment.newInstance(getDefaultTab());
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }

        // ask for permissions
        requestPermissionExternalStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();


        NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.registerDragStatusReceiver(this);

            // ask for permissions
            requestPermissionExternalStorage();

            /*
             * Check if the activity got an extra in its intend to show the nowplayingview directly.
             * If yes then pre set the dragoffset of the draggable helper.
             */
            if (mShowNPV) {
                nowPlayingView.setDragOffset(0.0f);

                // check preferences if the playlist should be shown
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

                final boolean showPlaylist = sharedPref.getBoolean(getString(R.string.pref_npv_show_playlist_key), getResources().getBoolean(R.bool.pref_npv_show_playlist_default));

                if (showPlaylist) {
                    mNowPlayingViewSwitcherStatus = VIEW_SWITCHER_STATUS.PLAYLIST_VIEW;
                    nowPlayingView.setViewSwitcherStatus(mNowPlayingViewSwitcherStatus);
                }
                mShowNPV = false;
            } else {
                // set drag status
                if (mSavedNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
                    nowPlayingView.setDragOffset(0.0f);
                } else if (mSavedNowPlayingDragStatus == DRAG_STATUS.DRAGGED_DOWN) {
                    nowPlayingView.setDragOffset(1.0f);
                }
                mSavedNowPlayingDragStatus = null;

                // set view switcher status
                if (mSavedNowPlayingViewSwitcherStatus != null) {
                    nowPlayingView.setViewSwitcherStatus(mSavedNowPlayingViewSwitcherStatus);
                    mNowPlayingViewSwitcherStatus = mSavedNowPlayingViewSwitcherStatus;
                }
                mSavedNowPlayingViewSwitcherStatus = null;
            }
            nowPlayingView.onResume();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // save drag status of the nowplayingview
        savedInstanceState.putInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS, mNowPlayingDragStatus.ordinal());

        // save the cover/playlist view status of the nowplayingview
        savedInstanceState.putInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW, mNowPlayingViewSwitcherStatus.ordinal());
    }

    @Override
    protected void onPause() {
        super.onPause();


        NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.registerDragStatusReceiver(null);

            nowPlayingView.onPause();
        }
    }

    @Override
    void onServiceConnected() {
        // the service is ready so check if odyssey was opened by a file
        checkUri();
    }

    @Override
    void onServiceDisconnected() {

    }

    @Override
    public void onBackPressed() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
            if (nowPlayingView != null) {
                View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                coordinatorLayout.setVisibility(View.VISIBLE);
                nowPlayingView.minimize();
            }
        } else if (fragmentManager.findFragmentById(R.id.fragment_container) instanceof FilesFragment) {
            // handle back pressed events for the files fragment manually

            FilesFragment fragment = (FilesFragment) fragmentManager.findFragmentById(R.id.fragment_container);

            if (fragment.isRootDirectory()) {
                // current directory is a root directory so handle back press normally
                super.onBackPressed();
            } else {
                if (fragmentManager.getBackStackEntryCount() == 0) {
                    // if backstack is empty but root directory not reached create an new fragment with the parent directory
                    List<String> storageVolumesList = mFileExplorerHelper.getStorageVolumes(getApplicationContext());

                    String parentDirectoryPath = fragment.getCurrentDirectory().getParent();

                    onDirectorySelected(parentDirectoryPath, storageVolumesList.contains(parentDirectoryPath), false);
                } else {
                    // back stack not empty so handle back press normally
                    super.onBackPressed();
                }
            }
        } else {
            super.onBackPressed();

            // enable navigation bar when backstack empty
            if (fragmentManager.getBackStackEntryCount() == 0) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        final FragmentManager fragmentManager = getSupportFragmentManager();

        if (item.getItemId() == android.R.id.home) {
            if (fragmentManager.findFragmentById(R.id.fragment_container) instanceof FilesFragment) {
                // handle click events for the files fragment manually

                final FilesFragment fragment = (FilesFragment) fragmentManager.findFragmentById(R.id.fragment_container);

                if (fragment.isRootDirectory()) {
                    // current directory is a root directory so enable navigation drawer

                    mDrawerToggle.setDrawerIndicatorEnabled(true);

                    if (mDrawerToggle.onOptionsItemSelected(item)) {
                        return true;
                    }
                } else {
                    if (fragmentManager.getBackStackEntryCount() == 0) {
                        // if backstack is empty but root directory not reached create an new fragment with the parent directory
                        final List<String> storageVolumesList = mFileExplorerHelper.getStorageVolumes(getApplicationContext());

                        final String parentDirectoryPath = fragment.getCurrentDirectory().getParent();

                        // don't add this this directory to the backstack
                        onDirectorySelected(parentDirectoryPath, storageVolumesList.contains(parentDirectoryPath), false);
                    } else {
                        // back stack not empty so just use the standard back press mechanism
                        onBackPressed();
                    }
                }

            } else if (fragmentManager.getBackStackEntryCount() > 0) {
                onBackPressed();
            } else {
                // back stack empty so enable navigation drawer

                mDrawerToggle.setDrawerIndicatorEnabled(true);

                if (mDrawerToggle.onOptionsItemSelected(item)) {
                    return true;
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.list_linear_listview && mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_menu_current_playlist, menu);

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

            try {
                if (getPlaybackService().getCurrentIndex() == info.position) {
                    menu.findItem(R.id.view_current_playlist_action_playnext).setVisible(false);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            CurrentPlaylistView currentPlaylistView = findViewById(R.id.now_playing_playlist);

            // check if track has a valid album id
            long albumId = currentPlaylistView.getAlbumId(info.position);
            AlbumModel tmpAlbum = MusicLibraryHelper.createAlbumModelFromId(albumId, getApplicationContext());

            menu.findItem(R.id.view_current_playlist_action_showalbum).setVisible(tmpAlbum != null);

            // check if track has a valid artist id
            String artistTitle = currentPlaylistView.getArtistTitle(info.position);
            long artistId = MusicLibraryHelper.getArtistIDFromName(artistTitle, this);

            menu.findItem(R.id.view_current_playlist_action_showartist).setVisible(artistId != -1);

            // check the view type
            if (currentPlaylistView.getItemViewType(info.position) == CurrentPlaylistAdapter.VIEW_TYPES.TYPE_SECTION_TRACK_ITEM) {
                menu.findItem(R.id.view_current_playlist_action_remove_section).setVisible(true);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();

        // we have two types of adapter context menuinfo classes so we have to make sure the current item contains the correct type of menuinfo
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

            CurrentPlaylistView currentPlaylistView = findViewById(R.id.now_playing_playlist);

            if (currentPlaylistView != null && mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
                final int itemId = item.getItemId();

                if (itemId == R.id.view_current_playlist_action_playnext) {
                    currentPlaylistView.enqueueTrackAsNext(info.position);
                    return true;
                } else if (itemId == R.id.view_current_playlist_action_remove_track) {
                    currentPlaylistView.removeTrack(info.position);
                    return true;
                } else if (itemId == R.id.view_current_playlist_action_remove_section) {
                    currentPlaylistView.removeSection(info.position);
                    return true;
                } else if (itemId == R.id.view_current_playlist_action_showalbum) {
                    long albumId = currentPlaylistView.getAlbumId(info.position);
                    AlbumModel tmpAlbum = MusicLibraryHelper.createAlbumModelFromId(albumId, getApplicationContext());

                    View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                    coordinatorLayout.setVisibility(View.VISIBLE);

                    NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
                    if (nowPlayingView != null) {
                        nowPlayingView.minimize();
                    }

                    onAlbumSelected(tmpAlbum, null);
                    return true;
                } else if (itemId == R.id.view_current_playlist_action_showartist) {
                    String artistTitle = currentPlaylistView.getArtistTitle(info.position);
                    long artistId = MusicLibraryHelper.getArtistIDFromName(artistTitle, this);

                    View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                    coordinatorLayout.setVisibility(View.VISIBLE);

                    NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
                    if (nowPlayingView != null) {
                        nowPlayingView.minimize();
                    }
                    onArtistSelected(new ArtistModel(artistTitle, artistId), null);
                    return true;
                }

                return super.onContextItemSelected(item);
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();

        final View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
        coordinatorLayout.setVisibility(View.VISIBLE);

        final NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.minimize();
        }

        // clear the searchmodel
        final SearchViewModel searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        searchViewModel.clearSearchString();

        final FragmentManager fragmentManager = getSupportFragmentManager();

        // clear backstack
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment fragment = null;

        if (id == R.id.nav_my_music) {
            fragment = MyMusicFragment.newInstance(getDefaultTab());
        } else if (id == R.id.nav_saved_playlists) {
            fragment = SavedPlaylistsFragment.newInstance();
        } else if (id == R.id.nav_bookmarks) {
            fragment = BookmarksFragment.newInstance();
        } else if (id == R.id.nav_files) {
            // open the default directory
            final List<String> storageVolumesList = mFileExplorerHelper.getStorageVolumes(getApplicationContext());

            String defaultDirectory = "/";

            if (!storageVolumesList.isEmpty()) {
                // choose the latest used storage volume as default
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                defaultDirectory = sharedPref.getString(getString(R.string.pref_file_browser_root_dir_key), storageVolumesList.get(0));
            }

            fragment = FilesFragment.newInstance(defaultDirectory, storageVolumesList.contains(defaultDirectory));
        } else if (id == R.id.nav_settings) {
            fragment = SettingsFragment.newInstance();
        } else if (id == R.id.nav_information) {
            fragment = InformationSettingsFragment.newInstance();
        }

        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }

        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        return true;
    }

    @Override
    public void onArtistSelected(ArtistModel artist, Bitmap bitmap) {
        // Create fragment and give it an argument for the selected article
        ArtistAlbumsFragment newFragment = ArtistAlbumsFragment.newInstance(artist, bitmap);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // set enter / exit animation
        newFragment.setEnterTransition(new Slide(Gravity.BOTTOM));
        newFragment.setExitTransition(new Slide(Gravity.TOP));

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("ArtistFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onAlbumSelected(AlbumModel album, Bitmap bitmap) {
        // Create fragment and give it an argument for the selected article
        AlbumTracksFragment newFragment = AlbumTracksFragment.newInstance(album, bitmap);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // set enter / exit animation
        newFragment.setEnterTransition(new Slide(Gravity.BOTTOM));
        newFragment.setExitTransition(new Slide(Gravity.TOP));

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("AlbumTracksFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onDirectorySelected(final String dirPath, final boolean isRootDirectory) {
        onDirectorySelected(dirPath, isRootDirectory, true);
    }

    private void onDirectorySelected(final String dirPath, final boolean isRootDirectory, final boolean addToBackStack) {
        // Create fragment and give it an argument for the selected directory
        final FilesFragment newFragment = FilesFragment.newInstance(dirPath, isRootDirectory);

        final FragmentManager fragmentManager = getSupportFragmentManager();

        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (!isRootDirectory) {
            // no root directory so set a enter / exit transition
            final int layoutDirection = getResources().getConfiguration().getLayoutDirection();
            newFragment.setEnterTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.START, layoutDirection)));
            newFragment.setExitTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.END, layoutDirection)));
        }

        transaction.replace(R.id.fragment_container, newFragment);
        if (!isRootDirectory && addToBackStack) {
            // add fragment only to the backstack if it's not a root directory
            transaction.addToBackStack("FilesFragment");
        }

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onStartSleepTimer(final long durationMS, final boolean stopAfterCurrent) {
        try {
            // save used duration to initialize the duration picker next time with this value
            SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            sharedPrefEditor.putLong(getString(R.string.pref_last_used_sleep_timer_key), durationMS);
            sharedPrefEditor.putBoolean(getString(R.string.pref_last_used_sleep_timer_stop_after_current_key), stopAfterCurrent);
            sharedPrefEditor.apply();

            getPlaybackService().startSleepTimer(durationMS, stopAfterCurrent);

            // show a snackbar to inform the user that the sleep timer is now set
            View layout = findViewById(R.id.drawer_layout);
            if (layout != null) {
                Snackbar sb = Snackbar.make(layout, R.string.snackbar_sleep_timer_confirmation_message, Snackbar.LENGTH_SHORT);
                // style the snackbar text
                TextView sbText = sb.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                sbText.setTextColor(ThemeUtils.getThemeColor(this, R.attr.odyssey_color_text_accent));
                sb.show();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(DRAG_STATUS status) {
        mNowPlayingDragStatus = status;
        if (status == DRAG_STATUS.DRAGGED_UP) {
            View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
            /*
             * Use View.GONE instead INVISIBLE to hide view behind NowPlayingView,
             * fixes overlaying Fragments on FragmentTransaction combined with minimizing the NPV in one action
             */
            coordinatorLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onSwitchedViews(VIEW_SWITCHER_STATUS view) {
        mNowPlayingViewSwitcherStatus = view;
    }

    @Override
    public void onStartDrag() {
        View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
        coordinatorLayout.setVisibility(View.VISIBLE);
    }

    /**
     * This method smoothly fades out the alpha value of the statusbar to give
     * a transition if the user pulls up the NowPlayingView.
     *
     * @param pos The position of the NowplayingView as float (in the range 0.0 - 1.0)
     */
    @Override
    public void onDragPositionChanged(float pos) {
        // Get the primary color of the active theme from the helper.
        int newColor = ThemeUtils.getThemeColor(this, R.attr.colorPrimary);

        // Calculate the offset depending on the floating point position (0.0-1.0 of the view)
        // Shift by 24 bit to set it as the A from ARGB and set all remaining 24 bits to 1 to
        int alphaOffset = (((255 - (int) (255.0 * pos)) << 24) | 0xFFFFFF);
        // and with this mask to set the new alpha value.
        newColor &= (alphaOffset);
        getWindow().setStatusBarColor(newColor);
    }

    @Override
    public void onPlaylistSelected(PlaylistModel playlistModel) {
        // Create fragment and give it an argument for the selected playlist
        PlaylistTracksFragment newFragment = PlaylistTracksFragment.newInstance(playlistModel);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // set enter / exit animation
        final int layoutDirection = getResources().getConfiguration().getLayoutDirection();
        newFragment.setEnterTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.START, layoutDirection)));
        newFragment.setExitTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.END, layoutDirection)));

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("PlaylistTracksFragment");

        // Commit the transaction
        transaction.commit();
    }

    private void requestPermissionExternalStorage() {
        // ask for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                View layout = findViewById(R.id.drawer_layout);
                if (layout != null) {
                    Snackbar sb = Snackbar.make(layout, R.string.permission_request_snackbar_explanation, Snackbar.LENGTH_INDEFINITE);
                    sb.setAction(R.string.permission_request_snackbar_button, view -> ActivityCompat.requestPermissions(OdysseyMainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE));
                    // style the snackbar text
                    TextView sbText = sb.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    sbText.setTextColor(ThemeUtils.getThemeColor(this, R.attr.odyssey_color_text_accent));
                    sb.show();
                }
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay!
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                if (fragment instanceof MyMusicFragment) {
                    ((MyMusicFragment) fragment).refresh();
                } else if (fragment instanceof OdysseyFragment) {
                    ((OdysseyFragment<?>) fragment).refreshContent();
                }
            }
        }
    }

    @Override
    public void onSaveObject(String title, SaveDialog.OBJECTTYPE type) {
        NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            // check type to identify which object should be saved
            switch (type) {
                case PLAYLIST:
                    nowPlayingView.savePlaylist(title);
                    break;
                case BOOKMARK:
                    nowPlayingView.createBookmark(title);
                    break;
            }
        }
    }

    @Override
    public void openArtworkSettings() {
        // Create fragment and give it an argument for the selected directory
        ArtworkSettingsFragment newFragment = ArtworkSettingsFragment.newInstance();

        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // set enter / exit animation
        final int layoutDirection = getResources().getConfiguration().getLayoutDirection();
        newFragment.setEnterTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.START, layoutDirection)));
        newFragment.setExitTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.END, layoutDirection)));

        transaction.addToBackStack("ArtworkSettingsFragment");

        transaction.replace(R.id.fragment_container, newFragment);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void setupFAB(View.OnClickListener listener) {
        FloatingActionButton playButton = findViewById(R.id.odyssey_play_button);

        if (playButton != null) {
            if (listener == null) {
                playButton.hide();
            } else {
                playButton.show();
            }

            playButton.setOnClickListener(listener);
        }
    }

    @Override
    public void setupToolbar(String title, boolean scrollingEnabled, boolean drawerIndicatorEnabled, boolean showImage) {
        // set drawer state
        mDrawerToggle.setDrawerIndicatorEnabled(drawerIndicatorEnabled);

        ImageView collapsingImage = findViewById(R.id.collapsing_image);
        View collapsingImageGradientTop = findViewById(R.id.collapsing_image_gradient_top);
        View collapsingImageGradientBottom = findViewById(R.id.collapsing_image_gradient_bottom);
        if (collapsingImage != null && collapsingImageGradientTop != null && collapsingImageGradientBottom != null) {
            if (showImage) {
                collapsingImage.setVisibility(View.VISIBLE);
                collapsingImageGradientTop.setVisibility(View.VISIBLE);
                collapsingImageGradientBottom.setVisibility(View.VISIBLE);
            } else {
                collapsingImage.setVisibility(View.GONE);
                collapsingImage.setImageDrawable(null);
                collapsingImageGradientTop.setVisibility(View.GONE);
                collapsingImageGradientBottom.setVisibility(View.GONE);
            }
        }
        // set scrolling behaviour
        CollapsingToolbarLayout toolbar = findViewById(R.id.collapsing_toolbar);
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
        params.height = -1;

        if (scrollingEnabled && !showImage) {
            toolbar.setTitleEnabled(false);
            setTitle(title);

            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL + AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED + AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        } else if (!scrollingEnabled && showImage && collapsingImage != null) {
            toolbar.setTitleEnabled(true);
            toolbar.setTitle(title);

            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED + AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        } else {
            toolbar.setTitleEnabled(false);
            setTitle(title);
            params.setScrollFlags(0);
        }
    }

    @Override
    public void setupToolbarImage(Bitmap bm) {
        ImageView collapsingImage = findViewById(R.id.collapsing_image);
        if (collapsingImage != null) {
            collapsingImage.setImageBitmap(bm);

            // FIXME DIRTY HACK: Manually fix the toolbar size to the screen width
            CollapsingToolbarLayout toolbar = findViewById(R.id.collapsing_toolbar);
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();

            params.height = getWindow().getDecorView().getMeasuredWidth();

            // Always expand the toolbar to show the complete image
            AppBarLayout appbar = findViewById(R.id.appbar);
            appbar.setExpanded(true, false);
        }
    }

    @Override
    public void onRecentAlbumsSelected() {
        // Create fragment
        RecentAlbumsFragment newFragment = RecentAlbumsFragment.newInstance();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // set enter / exit animation
        newFragment.setEnterTransition(new Slide(Gravity.BOTTOM));
        newFragment.setExitTransition(new Slide(Gravity.TOP));

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("RecentAlbumsFragment");

        // Commit the transaction
        transaction.commit();
    }

    private MyMusicFragment.DEFAULTTAB getDefaultTab() {
        // Read default view preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultView = sharedPref.getString(getString(R.string.pref_start_view_key), getString(R.string.pref_view_default));

        // the default tab for mymusic
        MyMusicFragment.DEFAULTTAB defaultTab = MyMusicFragment.DEFAULTTAB.ALBUMS;

        if (defaultView.equals(getString(R.string.pref_view_my_music_artists_key))) {
            defaultTab = MyMusicFragment.DEFAULTTAB.ARTISTS;
        } else if (defaultView.equals(getString(R.string.pref_view_my_music_albums_key))) {
            defaultTab = MyMusicFragment.DEFAULTTAB.ALBUMS;
        } else if (defaultView.equals(getString(R.string.pref_view_my_music_tracks_key))) {
            defaultTab = MyMusicFragment.DEFAULTTAB.TRACKS;
        }

        return defaultTab;
    }

    private int getDefaultViewID() {
        // Read default view preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultView = sharedPref.getString(getString(R.string.pref_start_view_key), getString(R.string.pref_view_default));

        // the nav resource id to mark the right item in the nav drawer
        int navId = -1;

        if (defaultView.equals(getString(R.string.pref_view_my_music_artists_key))) {
            navId = R.id.nav_my_music;
        } else if (defaultView.equals(getString(R.string.pref_view_my_music_albums_key))) {
            navId = R.id.nav_my_music;
        } else if (defaultView.equals(getString(R.string.pref_view_my_music_tracks_key))) {
            navId = R.id.nav_my_music;
        } else if (defaultView.equals(getString(R.string.pref_view_playlists_key))) {
            navId = R.id.nav_saved_playlists;
        } else if (defaultView.equals(getString(R.string.pref_view_bookmarks_key))) {
            navId = R.id.nav_bookmarks;
        } else if (defaultView.equals(getString(R.string.pref_view_files_key))) {
            navId = R.id.nav_files;
        }

        return navId;
    }

    /**
     * Check if odyssey was opened via a file.
     * <p>
     * This method will play the selected file if the mSentUri is valid.
     */
    private void checkUri() {
        if (mSentUri != null) {
            final String filePath = FileUtils.getFilePathFromUri(this, mSentUri);

            if (filePath != null) {
                try {
                    getPlaybackService().playURI(filePath);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                // show a snackbar to inform the user that the selected file could not be played
                final View layout = findViewById(R.id.drawer_layout);
                if (layout != null) {
                    final String errorMsg = getString(R.string.snackbar_uri_not_supported_message, mSentUri.toString());
                    final Snackbar sb = Snackbar.make(layout, errorMsg, Snackbar.LENGTH_SHORT);
                    // style the snackbar text
                    final TextView sbText = sb.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    sbText.setTextColor(ThemeUtils.getThemeColor(this, R.attr.odyssey_color_text_accent));
                    sb.show();
                }
            }

            mSentUri = null;
        }
    }
}
