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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;
import org.gateshipone.odyssey.utils.ThemeUtils;

public class MyMusicFragment extends Fragment implements TabLayout.OnTabSelectedListener {

    /**
     * Callback to setup toolbar and fab
     */
    protected ToolbarAndFABCallback mToolbarAndFABCallback;


    /**
     * ServiceConnection object to communicate with the PlaybackService
     */
    private PlaybackServiceConnection mServiceConnection;

    /**
     * Save the viewpager for later usage
     */
    private ViewPager mMyMusicViewPager;

    /**
     * Save the pageradapter for later usage
     */
    private MyMusicPagerAdapter mMyMusicPagerAdapter;

    /**
     * Save the searchview for later usage
     */
    private SearchView mSearchView;

    /**
     * Save the optionsmenu for later usage
     */
    private Menu mOptionMenu;

    /**
     * key value for arguments of the fragment
     */
    public final static String MY_MUSIC_REQUESTED_TAB = "ARG_REQUESTED_TAB";

    /**
     * enum for the default tab
     */
    public enum DEFAULTTAB {
        ARTISTS, ALBUMS, TRACKS
    }

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_my_music, container, false);

        // create tabs
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.my_music_tab_layout);

        // setup icons for tabs
        final ColorStateList tabColors = tabLayout.getTabTextColors();
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.ic_recent_actors_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }
        drawable = res.getDrawable(R.drawable.ic_album_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }
        drawable = res.getDrawable(R.drawable.ic_my_library_music_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // setup viewpager
        mMyMusicViewPager = (ViewPager) rootView.findViewById(R.id.my_music_viewpager);
        mMyMusicPagerAdapter = new MyMusicPagerAdapter(getChildFragmentManager());
        mMyMusicViewPager.setAdapter(mMyMusicPagerAdapter);
        mMyMusicViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mMyMusicViewPager.setOffscreenPageLimit(2);
        tabLayout.setOnTabSelectedListener(this);


        // set start page
        Bundle args = getArguments();

        DEFAULTTAB tab = DEFAULTTAB.ALBUMS;

        if (args != null) {
            tab = DEFAULTTAB.values()[args.getInt(MY_MUSIC_REQUESTED_TAB)];
        }

        switch (tab) {
            case ARTISTS:
                mMyMusicViewPager.setCurrentItem(0);
                break;
            case ALBUMS:
                mMyMusicViewPager.setCurrentItem(1);
                break;
            case TRACKS:
                mMyMusicViewPager.setCurrentItem(2);
                break;
        }

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_my_music), true, true, false);
            // set up play button
            mToolbarAndFABCallback.setupFAB(getPlayButtonListener(tab.ordinal()));
        }

        // activate options menu in toolbar
        setHasOptionsMenu(true);

        // set up pbs connection
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

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
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }
    }

    /**
     * Create a ClickListener for the play button if needed.
     */
    private View.OnClickListener getPlayButtonListener(int tab) {
        switch (tab) {
            case 0:
            case 1:
                return null;
            case 2:
                return new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // play all tracks on device
                        try {
                            mServiceConnection.getPBS().playAllTracks();
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
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

        OdysseyFragment fragment = mMyMusicPagerAdapter.getRegisteredFragment(tab.getPosition());
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
        OdysseyFragment fragment = mMyMusicPagerAdapter.getRegisteredFragment(tab.getPosition());

        // dismiss searchview
        if (mSearchView != null && mOptionMenu != null && !mSearchView.isIconified()) {
            if (mSearchView.getQuery().length() > 0) {
                // clear filter only if searchview contains text
                if (fragment != null) {
                    fragment.removeFilter();
                }
            }

            mSearchView.setIconified(true);
            mOptionMenu.findItem(R.id.action_search).collapseActionView();
        }

        if (null != fragment) {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_my_music, menu);

        mOptionMenu = menu;

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        mOptionMenu.findItem(R.id.action_search).setIcon(drawable);

        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        mSearchView.setOnQueryTextListener(new SearchTextObserver());

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Custom pager adapter to retrieve already registered fragments.
     */
    private class MyMusicPagerAdapter extends FragmentStatePagerAdapter {
        static final int NUMBER_OF_PAGES = 3;

        private SparseArray<OdysseyFragment> mRegisteredFragments;

        public MyMusicPagerAdapter(FragmentManager fm) {
            super(fm);
            mRegisteredFragments = new SparseArray<>();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            OdysseyFragment fragment = (OdysseyFragment) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mRegisteredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new ArtistsFragment();
                case 1:
                    return new AlbumsFragment();
                case 2:
                    return new AllTracksFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // this is done in order to reload all tabs
            return NUMBER_OF_PAGES;
        }

        public OdysseyFragment getRegisteredFragment(int position) {
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

            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            applyFilter(newText);

            return true;
        }

        private void applyFilter(String filter) {
            OdysseyFragment fragment = mMyMusicPagerAdapter.getRegisteredFragment(mMyMusicViewPager.getCurrentItem());

            if (filter.isEmpty()) {
                fragment.removeFilter();
            } else {
                fragment.applyFilter(filter);
            }
        }
    }
}
