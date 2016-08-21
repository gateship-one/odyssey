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

package org.odyssey.fragments;

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

import org.odyssey.activities.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.ThemeUtils;

public class MyMusicFragment extends OdysseyFragment implements TabLayout.OnTabSelectedListener {

    private PlaybackServiceConnection mServiceConnection;

    private ViewPager mMyMusicViewPager;

    private MyMusicPagerAdapter mMyMusicPagerAdapter;

    private SearchView mSearchView;

    private Menu mOptionMenu;

    public final static String MY_MUSIC_REQUESTED_TAB = "ARG_REQUESTED_TAB";

    public enum DEFAULTTAB {
        ARTISTS, ALBUMS, TRACKS
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_my_music, container, false);

        // create tabs
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.my_music_tab_layout);

        // Icons
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

        mMyMusicViewPager = (ViewPager) rootView.findViewById(R.id.my_music_viewpager);
        mMyMusicPagerAdapter = new MyMusicPagerAdapter(getChildFragmentManager());
        mMyMusicViewPager.setAdapter(mMyMusicPagerAdapter);
        mMyMusicViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
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

        setHasOptionsMenu(true);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_my_music), true, true, false);
        // set up play button
        activity.setUpPlayButton(null);

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

        return rootView;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        View view = this.getView();

        if (view != null) {
            if (!mSearchView.isIconified()) {
                mSearchView.setIconified(true);
                mOptionMenu.findItem(R.id.action_search).collapseActionView();
            }

            ViewPager myMusicViewPager = (ViewPager) view.findViewById(R.id.my_music_viewpager);
            myMusicViewPager.setCurrentItem(tab.getPosition());

            View.OnClickListener listener = null;

            switch (tab.getPosition()) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // play all tracks on device
                            try {
                                mServiceConnection.getPBS().playAllTracksShuffled();
                            } catch (RemoteException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    };
                    break;
            }

            OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
            activity.setUpPlayButton(listener);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public void refresh() {
        // reload tabs
        View view = this.getView();

        if (view != null) {
            ViewPager myMusicViewPager = (ViewPager) view.findViewById(R.id.my_music_viewpager);
            myMusicViewPager.getAdapter().notifyDataSetChanged();
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
        menuInflater.inflate(R.menu.options_menu_my_music, menu);

        mOptionMenu = menu;

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), android.R.attr.textColor);

        Drawable drawable = menu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        mOptionMenu.findItem(R.id.action_search).setIcon(drawable);

        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        mSearchView.setOnQueryTextListener(new SearchTextObserver());
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                OdysseyFragment fragment = mMyMusicPagerAdapter.getRegisteredFragment(mMyMusicViewPager.getCurrentItem());
                if (fragment != null) {
                    fragment.removeFilter();
                }
                return false;
            }
        });

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
