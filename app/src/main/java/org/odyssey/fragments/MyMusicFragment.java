package org.odyssey.fragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;

public class MyMusicFragment extends Fragment implements TabLayout.OnTabSelectedListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_my_music, container, false);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_my_music), true, true);

        // create tabs
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.my_music_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.fragment_title_artists));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.fragment_title_albums));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.fragment_title_all_tracks));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        ViewPager myMusicViewPager = (ViewPager) rootView.findViewById(R.id.my_music_viewpager);
        MyMusicPagerAdapter adapterViewPager = new MyMusicPagerAdapter(getChildFragmentManager());
        myMusicViewPager.setAdapter(adapterViewPager);
        myMusicViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(this);

        // set start page to albums
        myMusicViewPager.setCurrentItem(1);

        return rootView;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        View view = this.getView();

        if(view != null) {
            ViewPager myMusicViewPager = (ViewPager) this.getView().findViewById(R.id.my_music_viewpager);
            myMusicViewPager.setCurrentItem(tab.getPosition());
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    public static class MyMusicPagerAdapter extends FragmentStatePagerAdapter {
        static final int NUMBER_OF_PAGES = 3;

        public MyMusicPagerAdapter(FragmentManager fm) {
            super(fm);
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
            return NUMBER_OF_PAGES;
        }
    }
}
