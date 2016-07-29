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

package org.odyssey.adapter;

import android.content.Context;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.FormatHelper;
import org.odyssey.views.CurrentPlaylistViewItem;

public class CurrentPlaylistListViewAdapter extends BaseAdapter {

    private final Context mContext;
    private final PlaybackServiceConnection mPlayBackServiceConnection;

    private int mCurrentPlayingIndex = -1;
    private int mPlaylistSize = 0;

    public CurrentPlaylistListViewAdapter(Context context, PlaybackServiceConnection playbackServiceConnection) {
        super();

        mContext = context;

        mPlayBackServiceConnection = playbackServiceConnection;

        try {
            mPlaylistSize = mPlayBackServiceConnection.getPBS().getPlaylistSize();
            mCurrentPlayingIndex = mPlayBackServiceConnection.getPBS().getCurrentIndex();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the length of the model data of this adapter.
     */
    @Override
    public int getCount() {
        return mPlaylistSize;
    }

    /**
     * Simple getter for the model data.
     * This method will call the PBS to get the trackmodel from the current playlist.
     *
     * @param position Index of the track to get. No check for boundaries here.
     * @return The trackmodel at index position.
     */
    @Override
    public Object getItem(int position) {
        try {
            if (mPlayBackServiceConnection != null) {
                return mPlayBackServiceConnection.getPBS().getPlaylistSong(position);
            } else {
                return null;
            }
        } catch (RemoteException e) {
            return null;
        }
    }

    /**
     * Simple position->id mapping here.
     *
     * @param position Position to get the id from
     * @return The id (position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     *
     * @param position    The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent      The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // get the trackmodel for the current position from the PBS
        TrackModel track;
        try {
            if (mPlayBackServiceConnection != null) {
                track = mPlayBackServiceConnection.getPBS().getPlaylistSong(position);
            } else {
                track = new TrackModel();
            }
        } catch (RemoteException e) {
            track = new TrackModel();
        }

        // title
        String trackTitle = track.getTrackName();

        // additional information (artist + album)
        String trackInformation = track.getTrackArtistName() + " - " + track.getTrackAlbumName();

        // tracknumber
        String trackNumber = FormatHelper.formatTrackNumber(track.getTrackNumber());

        // duration
        String trackDuration = FormatHelper.formatTracktimeFromMS(track.getTrackDuration());

        if (convertView != null) {
            CurrentPlaylistViewItem currentPlaylistViewItem = (CurrentPlaylistViewItem) convertView;
            currentPlaylistViewItem.setNumber(trackNumber);
            currentPlaylistViewItem.setTitle(trackTitle);
            currentPlaylistViewItem.setAdditionalInformation(trackInformation);
            currentPlaylistViewItem.setDuration(trackDuration);
        } else {
            convertView = new CurrentPlaylistViewItem(mContext, trackNumber, trackTitle, trackInformation, trackDuration);
        }

        if (position == mCurrentPlayingIndex) {
            ((CurrentPlaylistViewItem) convertView).setPlaying(true);
        } else {
            ((CurrentPlaylistViewItem) convertView).setPlaying(false);
        }

        return convertView;
    }

    /**
     * Update the playlist size and the index of the current track.
     *
     * @param info The NowplayingInformation object containing the new playlist size and the current track index
     */
    public void updateState(NowPlayingInformation info) {
        mCurrentPlayingIndex = info.getPlayingIndex();
        mPlaylistSize = info.getPlaylistLength();
        notifyDataSetChanged();
    }
}
