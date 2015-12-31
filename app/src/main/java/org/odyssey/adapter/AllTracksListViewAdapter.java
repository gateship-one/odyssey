package org.odyssey.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import org.odyssey.models.TrackModel;
import org.odyssey.views.AllTracksListViewItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AllTracksListViewAdapter extends BaseAdapter implements SectionIndexer {

    private Context mContext;

    private List<TrackModel> mTracks;

    ArrayList<String> mSectionList;
    ArrayList<Integer> mSectionPositions;
    HashMap<Character, Integer> mPositionSectionMap;

    public AllTracksListViewAdapter(Context context) {
        super();

        mContext = context;

        mTracks = new ArrayList<>();

        mSectionList = new ArrayList<String>();
        mSectionPositions = new ArrayList<Integer>();
        mPositionSectionMap = new HashMap<Character, Integer>();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sectionIndex >= 0 && sectionIndex < mSectionPositions.size()) {
            return mSectionPositions.get(sectionIndex);
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int pos) {

        TrackModel track = (TrackModel) getItem(pos);

        String trackName = track.getTrackName();

        char trackSection = trackName.toUpperCase().charAt(0);
        if (mPositionSectionMap.containsKey(trackSection)) {
            int sectionIndex = mPositionSectionMap.get(trackSection);
            return sectionIndex;
        }

        return 0;
    }

    @Override
    public Object[] getSections() {

        return mSectionList.toArray();
    }

    @Override
    public int getCount() {
        return mTracks.size();
    }

    @Override
    public Object getItem(int position) {
        return mTracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to call.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param tracks
     *            Actual model data
     */
    public void swapModel(List<TrackModel> tracks) {
        if (tracks == null) {
            mTracks.clear();
        } else {
            mTracks = tracks;
        }
        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mTracks.size() > 0) {
            char lastSection = 0;

            TrackModel currentTrack = mTracks.get(0);

            lastSection = currentTrack.getTrackName().toUpperCase().charAt(0);

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentTrack = mTracks.get(i);

                char currentSection = currentTrack.getTrackName().toUpperCase().charAt(0);

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
        notifyDataSetChanged();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TrackModel track = mTracks.get(position);

        // title
        String trackTitle = track.getTrackName();

        // additional information (artist + album)
        String trackInformation = track.getTrackArtistName() + " - " + track.getTrackAlbumName();

        // tracknumber
        String trackNumber = String.valueOf(track.getTrackNumber());
        if(trackNumber.length() >= 4) {
            trackNumber = trackNumber.substring(2);
        }
        // duration
        String seconds = String.valueOf((track.getTrackDuration() % 60000) / 1000);
        if(seconds.length() == 1) {
            seconds = "0" + seconds;
        }

        String minutes = String.valueOf(track.getTrackDuration() / 60000);

        String trackDuration = minutes + ":" + seconds;

        if(convertView != null) {
            AllTracksListViewItem allTracksListViewItem = (AllTracksListViewItem) convertView;
            allTracksListViewItem.setNumber(trackNumber);
            allTracksListViewItem.setTitle(trackTitle);
            allTracksListViewItem.setAdditionalInformation(trackInformation);
            allTracksListViewItem.setDuration(trackDuration);
        } else {
            convertView = new AllTracksListViewItem(mContext, trackNumber, trackTitle, trackInformation, trackDuration);
        }

        return convertView;
    }
}
