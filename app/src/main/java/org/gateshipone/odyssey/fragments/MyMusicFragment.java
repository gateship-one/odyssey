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

package org.gateshipone.odyssey.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.listener.OnRecentAlbumsSelectedListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.SearchViewModel;

public class MyMusicFragment extends Fragment implements TabLayout.OnTabSelectedListener {

    /**
     * Callback to open the recent albums fragment
     */
    private OnRecentAlbumsSelectedListener mRecentAlbumsSelectedListener;

    /**
     * Callback to setup toolbar and fab
     */
    protected ToolbarAndFABCallback mToolbarAndFABCallback;

    /**
     * Save the viewpager for later usage
     */
    private ViewPager mMyMusicViewPager;

    /**
     * Save the pageradapter for later usage
     */
    private MyMusicPagerAdapter mMyMusicPagerAdapter;

    /**
     * Save the optionsmenu for later usage
     */
    private Menu mOptionMenu;

    /**
     * Saved search string when user rotates devices
     */
    private String mSearchString;

    /**
     * Save a searchview reference for later usage
     */
    private SearchView mSearchView;

    /**
     * Constant for state saving
     */
    public static final String MYMUSICFRAGMENT_SAVED_INSTANCE_SEARCH_STRING = "MyMusicFragment.SearchString";

    /**
     * key value for arguments of the fragment
     */
    private static final String ARG_REQUESTED_TAB = "requested_tab";

    /**
     * enum for the default tab
     */
    public enum DEFAULTTAB {
        ARTISTS, ALBUMS, TRACKS
    }

    public static MyMusicFragment newInstance(final DEFAULTTAB defaulttab) {
        final Bundle args = new Bundle();
        args.putInt(ARG_REQUESTED_TAB, defaulttab.ordinal());

        final MyMusicFragment fragment = new MyMusicFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // create tabs
        final TabLayout tabLayout = view.findViewById(R.id.my_music_tab_layout);

        // setup viewpager
        mMyMusicViewPager = view.findViewById(R.id.my_music_viewpager);
        mMyMusicPagerAdapter = new MyMusicPagerAdapter(getChildFragmentManager());
        mMyMusicViewPager.setAdapter(mMyMusicPagerAdapter);
        mMyMusicViewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(mMyMusicViewPager, false);
        tabLayout.addOnTabSelectedListener(this);

        // setup icons for tabs
        final ColorStateList tabColors = tabLayout.getTabTextColors();
        final Resources res = getResources();
        Drawable drawable = null;
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            switch (i) {
                case 0:
                    drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_recent_actors_24dp, null);
                    break;
                case 1:
                    drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_album_24dp, null);
                    break;
                case 2:
                    drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_my_library_music_24dp, null);
                    break;
            }

            if (drawable != null) {
                Drawable icon = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(icon, tabColors);
                tabLayout.getTabAt(i).setIcon(icon);
            }
        }
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // try to resume the saved search string
        if (savedInstanceState != null) {
            mSearchString = savedInstanceState.getString(MYMUSICFRAGMENT_SAVED_INSTANCE_SEARCH_STRING);
        }

        // activate options menu in toolbar
        setHasOptionsMenu(true);

        // set start page
        final Bundle args = getArguments();

        // only set requested tab if no state was saved
        if (args != null && savedInstanceState == null) {
            final DEFAULTTAB tab = DEFAULTTAB.values()[args.getInt(ARG_REQUESTED_TAB)];

            switch (tab) {
                case ARTISTS:
                    mMyMusicViewPager.setCurrentItem(0, false);
                    break;
                case ALBUMS:
                    mMyMusicViewPager.setCurrentItem(1, false);
                    break;
                case TRACKS:
                    mMyMusicViewPager.setCurrentItem(2, false);
                    break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // save the already typed search string (or null if nothing is entered)
        outState.putString(MYMUSICFRAGMENT_SAVED_INSTANCE_SEARCH_STRING, mSearchString);
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interfaces. If not, it throws an exception

        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }

        try {
            mRecentAlbumsSelectedListener = (OnRecentAlbumsSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnRecentAlbumsSelectedListener");
        }

        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(new SearchTextObserver());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(null);
        }
    }

    /**
     * Setup toolbar and button callback in last creation state.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set up play button
            mToolbarAndFABCallback.setupFAB(getPlayButtonListener(mMyMusicViewPager.getCurrentItem()));
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_my_music), true, true, false);
        }
    }

    /**
     * Create a ClickListener for the play button if needed.
     */
    private View.OnClickListener getPlayButtonListener(int tab) {
        switch (tab) {
            case 0:
            case 1:
                // add logic here if necessary
                return null;
            case 2:
                return v -> {
                    // play all tracks on device
                    try {
                        ((GenericActivity) requireActivity()).getPlaybackService().playAllTracks(mSearchString);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                };
            default:
                return null;
        }
    }

    /**
     * Called when a tab enters the selected state.
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        // set viewpager to current page
        mMyMusicViewPager.setCurrentItem(tab.getPosition());

        if (mToolbarAndFABCallback != null) {
            // show fab only for AllTracksFragment
            View.OnClickListener listener = getPlayButtonListener(tab.getPosition());

            // set up play button
            mToolbarAndFABCallback.setupFAB(listener);
        }

        if (mOptionMenu != null) {
            // show recents options only for the albums fragment
            final MenuItem item = mOptionMenu.findItem(R.id.action_show_recent_albums);
            if (item != null) {
                item.setVisible(mMyMusicViewPager.getCurrentItem() == 1);
            }
        }

        final OdysseyFragment<?> fragment = mMyMusicPagerAdapter.getRegisteredFragment(tab.getPosition());

        if (fragment != null) {
            fragment.getContent();

            // Disable memory trimming to prevent removing the shown data
            fragment.enableMemoryTrimming(false);
        }
    }

    /**
     * Called when a tab leaves the selected state.
     * <p/>
     * This method will take care of dismissing the searchview and showing the fab.
     */
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        final OdysseyFragment<?> fragment = mMyMusicPagerAdapter.getRegisteredFragment(tab.getPosition());

        if (fragment != null) {
            // Reenable memory trimming now, because the Fragment is hidden
            fragment.enableMemoryTrimming(true);
        }
    }


    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    /**
     * Method to reload the fragments.
     */
    public void refresh() {
        // reload tabs
        mMyMusicPagerAdapter.notifyDataSetChanged();
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_my_music, menu);

        mOptionMenu = menu;

        // get tint color
        final int tintColor = ThemeUtils.getThemeColor(requireContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = mOptionMenu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        mOptionMenu.findItem(R.id.action_search).setIcon(drawable);

        mSearchView = (SearchView) mOptionMenu.findItem(R.id.action_search).getActionView();

        // Check if a search string is saved from before
        if (mSearchString != null) {
            // Expand the view
            mSearchView.setIconified(false);
            mOptionMenu.findItem(R.id.action_search).expandActionView();
            // Set the query string
            mSearchView.setQuery(mSearchString, true);
        }

        mSearchView.setOnQueryTextListener(new SearchTextObserver());

        // show recents options only for the albums fragment
        mOptionMenu.findItem(R.id.action_show_recent_albums).setVisible(mMyMusicViewPager.getCurrentItem() == 1);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_show_recent_albums) {
            mRecentAlbumsSelectedListener.onRecentAlbumsSelected();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private SearchViewModel getViewModel() {
        return new ViewModelProvider(this).get(SearchViewModel.class);
    }

    /**
     * Custom pager adapter to retrieve already registered fragments.
     */
    private static class MyMusicPagerAdapter extends FragmentStatePagerAdapter {
        static final int NUMBER_OF_PAGES = 3;

        private final SparseArray<OdysseyFragment<?>> mRegisteredFragments;

        public MyMusicPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mRegisteredFragments = new SparseArray<>();
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            OdysseyFragment<?> fragment = (OdysseyFragment<?>) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            mRegisteredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return ArtistsFragment.newInstance();
                case 1:
                    return AlbumsFragment.newInstance();
                case 2:
                    return AllTracksFragment.newInstance();
                default:
                    // should not happen throw exception
                    throw new IllegalStateException("No fragment defined to return");
            }
        }

        @Override
        public int getCount() {
            // this is done in order to reload all tabs
            return NUMBER_OF_PAGES;
        }

        public OdysseyFragment<?> getRegisteredFragment(int position) {
            return mRegisteredFragments.get(position);
        }
    }

    /**
     * Observer class to apply a filter to the current fragment in the viewpager.
     */
    private class SearchTextObserver implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            applyFilter(query);

            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            applyFilter(newText);

            return true;
        }

        private void applyFilter(String filter) {

            SearchViewModel searchViewModel = getViewModel();

            if (filter.isEmpty()) {
                mSearchString = null;
                searchViewModel.clearSearchString();
            } else {
                mSearchString = filter;
                searchViewModel.setSearchString(filter);
            }
        }
    }
}
