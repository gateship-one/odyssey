package org.odyssey.fragments;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.playbackservice.PlaybackServiceConnection;

public class MyMusicFragment extends OdysseyFragment implements TabLayout.OnTabSelectedListener {

    private PlaybackServiceConnection mServiceConnection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_my_music, container, false);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_my_music), true, true,false);

        // create tabs
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.my_music_tab_layout);

        // Icons
        final ColorStateList tabColors = tabLayout.getTabTextColors();
        Drawable icon = getResources().getDrawable(R.drawable.ic_recent_actors_24dp);
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTintList(icon,tabColors);
        tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        icon = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_album_24dp));
        DrawableCompat.setTintList(icon, tabColors);
        tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        icon = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_my_library_music_24dp));
        DrawableCompat.setTintList(icon, tabColors);
        tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        ViewPager myMusicViewPager = (ViewPager) rootView.findViewById(R.id.my_music_viewpager);
        MyMusicPagerAdapter adapterViewPager = new MyMusicPagerAdapter(getChildFragmentManager());
        myMusicViewPager.setAdapter(adapterViewPager);
        myMusicViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(this);

        // set start page to albums
        myMusicViewPager.setCurrentItem(1);

        // set up play button
        activity.setUpPlayButton(null);

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();

        return rootView;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        View view = this.getView();

        if(view != null) {
            ViewPager myMusicViewPager = (ViewPager) view.findViewById(R.id.my_music_viewpager);
            myMusicViewPager.setCurrentItem(tab.getPosition());

            View.OnClickListener listener = null;

            switch(tab.getPosition()) {
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

    public static class MyMusicPagerAdapter extends FragmentStatePagerAdapter {
        static final int NUMBER_OF_PAGES = 3;

        public MyMusicPagerAdapter(FragmentManager fm) {
            super(fm);
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
    }
}
