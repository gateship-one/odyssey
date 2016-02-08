package org.odyssey;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import org.odyssey.fragments.AlbumTracksFragment;
import org.odyssey.fragments.ArtistAlbumsFragment;
import org.odyssey.fragments.MyMusicFragment;
import org.odyssey.fragments.PlaylistTracksFragment;
import org.odyssey.fragments.SavedPlaylistsFragment;
import org.odyssey.fragments.SettingsFragment;
import org.odyssey.listener.OnAlbumSelectedListener;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.listener.OnPlaylistSelectedListener;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;
import org.odyssey.views.CurrentPlaylistView;
import org.odyssey.views.NowPlayingView;

import java.util.ArrayList;

public class OdysseyMainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnArtistSelectedListener, OnAlbumSelectedListener, OnPlaylistSelectedListener, NowPlayingView.NowPlayingDragStatusReceiver{

    private ActionBarDrawerToggle mDrawerToggle;

    private DRAG_STATUS mNowPlayingDragStatus;

    public final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW = "org.odyssey.requestedview";
    public final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW = "org.odyssey.requestedview.nowplaying";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Read theme preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPref.getString("pref_theme","indigo");

        switch (themePref) {
            case "indigo":
                setTheme(R.style.AppTheme_indigo);
                break;
            case "orange":
                setTheme(R.style.AppTheme_orange);
                break;
            case "deeporange":
                setTheme(R.style.AppTheme_deepOrange);
                break;
            case "blue":
                setTheme(R.style.AppTheme_blue);
                break;
            case "darkgrey":
                setTheme(R.style.AppTheme_darkGrey);
                break;
            case "brown":
                setTheme(R.style.AppTheme_brown);
                break;
        }

        super.onCreate(savedInstanceState);

        // ask for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        setContentView(R.layout.activity_odyssey_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // enable back navigation
        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        if(actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_my_music);

        // register context menu for currenPlaylistListView
        ListView currentPlaylistListView = (ListView) findViewById(R.id.current_playlist_listview);
        registerForContextMenu(currentPlaylistListView);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                return;
            }

            Fragment myMusicFragment = new MyMusicFragment();

            myMusicFragment.setArguments(getIntent().getExtras());

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragment_container, myMusicFragment);
            transaction.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        nowPlayingView.registerDragStatusReceiver(this);

        /*
         * Check if the activity got an extra in its intend to show the nowplayingview directly.
         * If yes then pre set the dragoffset of the draggable helper.
         */
        Intent resumeIntent = getIntent();
        if (resumeIntent != null && resumeIntent.getExtras() != null &&
                resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW) != null &&
                resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW).equals(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW)) {
            nowPlayingView.setDragOffset(0.0f);
            getIntent().removeExtra(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW);
        }
        nowPlayingView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        nowPlayingView.registerDragStatusReceiver(null);

        nowPlayingView.onPause();
    }

    @Override
    public void onBackPressed() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
            nowPlayingView.minimize();
        } else if ( fragmentManager.findFragmentById(R.id.fragment_container) instanceof SettingsFragment || fragmentManager.findFragmentById(R.id.fragment_container) instanceof SavedPlaylistsFragment) {
            // If current fragment is the settings or savedplaylists fragment, jump back to myMusicFragment.
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new MyMusicFragment());
            transaction.commit();

            // Reset the navigation view
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_my_music);
        }
        else {
            super.onBackPressed();

            // enable navigation bar when backstack empty
            if (fragmentManager.getBackStackEntryCount() == 0) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.odyssey_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (item.getItemId()) {
            case android.R.id.home:
                if(fragmentManager.getBackStackEntryCount() > 0) {
                    onBackPressed();
                } else {
                    // back stack empty so enable navigation drawer

                    mDrawerToggle.setDrawerIndicatorEnabled(true);

                    if(mDrawerToggle.onOptionsItemSelected(item)) {
                        return true;
                    }
                }
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.current_playlist_listview) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_menu_current_playlist, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        CurrentPlaylistView currentPlaylistView = (CurrentPlaylistView) findViewById(R.id.now_playing_playlist);

        switch (item.getItemId()) {
            case R.id.view_current_playlist_action_playnext:
                currentPlaylistView.enqueueTrackAsNext(info.position);
                return true;
            case R.id.view_current_playlist_action_remove:
                currentPlaylistView.removeTrack(info.position);
                return true;
            case R.id.view_current_playlist_action_showalbum:
                String albumKey = currentPlaylistView.getAlbumKey(info.position);
                ArrayList<String> albumInformations = MusicLibraryHelper.getAlbumInformationFromKey(albumKey, this);
                if (albumInformations.size() == 3) {
                    NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
                    nowPlayingView.minimize();
                    onAlbumSelected(albumKey, albumInformations.get(0), albumInformations.get(1), albumInformations.get(2));
                }
                return true;
            case R.id.view_current_playlist_action_showartist:
                String artistTitle = currentPlaylistView.getArtistTitle(info.position);
                long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, this);
                NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
                nowPlayingView.minimize();
                onArtistSelected(artistTitle, artistID);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        nowPlayingView.minimize();

        FragmentManager fragmentManager = getSupportFragmentManager();

        // clear backstack
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment fragment = null;

        if (id == R.id.nav_my_music) {
            fragment = new MyMusicFragment();
        } else if (id == R.id.nav_saved_playlists) {
            fragment = new SavedPlaylistsFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        return true;
    }

    @Override
    public void onArtistSelected(String artist, long artistID) {
        // Create fragment and give it an argument for the selected article
        ArtistAlbumsFragment newFragment = new ArtistAlbumsFragment();
        Bundle args = new Bundle();
        args.putString(ArtistAlbumsFragment.ARG_ARTISTNAME, artist);
        args.putLong(ArtistAlbumsFragment.ARG_ARTISTID, artistID);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
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
    public void onAlbumSelected(String albumKey, String albumTitle, String albumArtURL, String artistName) {
        // Create fragment and give it an argument for the selected article
        AlbumTracksFragment newFragment = new AlbumTracksFragment();
        Bundle args = new Bundle();
        args.putString(AlbumTracksFragment.ARG_ALBUMKEY, albumKey);
        args.putString(AlbumTracksFragment.ARG_ALBUMTITLE, albumTitle);
        args.putString(AlbumTracksFragment.ARG_ALBUMART, albumArtURL);
        args.putString(AlbumTracksFragment.ARG_ALBUMARTIST, artistName);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("AlbumTracksFragment");

        // Commit the transaction
        transaction.commit();
    }

    public void setUpToolbar(String title, boolean scrollingEnabled, boolean drawerIndicatorEnabled, boolean showImage) {

        // set drawer state
        mDrawerToggle.setDrawerIndicatorEnabled(drawerIndicatorEnabled);


        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        if ( showImage ) {
            collapsingImage.setVisibility(View.VISIBLE);
        } else {
            collapsingImage.setVisibility(View.GONE);
        }
        // set scrolling behaviour
        CollapsingToolbarLayout toolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        // set title for both the activity and the collapsingToolbarlayout for both cases
        // where and image is shown and not.
        toolbar.setTitle(title);
        setTitle(title);


        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
//
        AppBarLayout layout = (AppBarLayout) findViewById(R.id.appbar);
        layout.setExpanded(true, false);
        if (scrollingEnabled) {
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        } else {
            params.setScrollFlags(0);
        }

        if ( showImage ) {
            // Enable title of collapsingToolbarlayout for smooth transition
            toolbar.setTitleEnabled(true);
            setToolbarImage(getResources().getDrawable(R.drawable.cover_placeholder));
            params.setScrollFlags(params.getScrollFlags() | AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL |AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED );
        } else {
            // Disable title for collapsingToolbarLayout and show normal title
            toolbar.setTitleEnabled(false);
        }
    }

    public void setToolbarImage(Bitmap bm ) {
        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        collapsingImage.setImageBitmap(bm);
    }

    public void setToolbarImage(Drawable drawable ) {
        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        collapsingImage.setImageDrawable(drawable);
    }

    public void setUpPlayButton(View.OnClickListener listener) {
        FloatingActionButton playButton = (FloatingActionButton) findViewById(R.id.odyssey_play_button);

        if(listener == null) {
            playButton.hide();
        } else {
            playButton.show();
        }

        playButton.setOnClickListener(listener);
    }

    @Override
    public void onStatusChanged(DRAG_STATUS status) {
        mNowPlayingDragStatus = status;
    }

    @Override
    public void onPlaylistSelected(String playlistTitle, long playlistID) {
        // Create fragment and give it an argument for the selected playlist
        PlaylistTracksFragment newFragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putString(PlaylistTracksFragment.ARG_PLAYLISTTITLE, playlistTitle);
        args.putLong(PlaylistTracksFragment.ARG_PLAYLISTID, playlistID);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("PlaylistTracksFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionHelper.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                } else {

                    // permission denied, boo!
                }
                break;
            }
        }
    }
}
