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

package org.gateshipone.odyssey.adapter;

import android.content.Context;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.viewitems.CurrentPlaylistItem;
import org.gateshipone.odyssey.viewitems.ListViewItem;

import java.util.List;

public class CurrentPlaylistAdapter extends ScrollSpeedAdapter {

    private final Context mContext;
    private final PlaybackServiceConnection mPlayBackServiceConnection;

    private int mCurrentPlayingIndex = -1;
    private int mPlaylistSize = 0;

    private final ArtworkManager mArtworkManager;

    public CurrentPlaylistAdapter(Context context, PlaybackServiceConnection playbackServiceConnection) {
        super();

        mContext = context;

        mPlayBackServiceConnection = playbackServiceConnection;

        try {
            mPlaylistSize = mPlayBackServiceConnection.getPBS().getPlaylistSize();
            mCurrentPlayingIndex = mPlayBackServiceConnection.getPBS().getCurrentIndex();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mArtworkManager = ArtworkManager.getInstance(context.getApplicationContext());
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
        TrackModel currentTrack;
        TrackModel previousTrack;
        try {
            if (mPlayBackServiceConnection != null) {
                currentTrack = mPlayBackServiceConnection.getPBS().getPlaylistSong(position);
                if (position > 0) {
                    previousTrack = mPlayBackServiceConnection.getPBS().getPlaylistSong(position - 1);
                } else {
                    previousTrack = null;
                }
            } else {
                currentTrack = new TrackModel();
                previousTrack = null;
            }
        } catch (RemoteException e) {
            currentTrack = new TrackModel();
            previousTrack = null;
        }

        // title (number + name)
        String trackTitle = currentTrack.getTrackName();
        String trackNumber = FormatHelper.formatTrackNumber(currentTrack.getTrackNumber());
        if (!trackTitle.isEmpty() && !trackNumber.isEmpty()) {
            trackTitle = mContext.getString(R.string.track_title_template, trackNumber, trackTitle);
        } else if (!trackNumber.isEmpty()) {
            trackTitle = trackNumber;
        }

        // subtitle (artist + album)
        String trackSubtitle = currentTrack.getTrackAlbumName();
        if (!currentTrack.getTrackArtistName().isEmpty() && !trackSubtitle.isEmpty()) {
            trackSubtitle = mContext.getString(R.string.track_title_template, currentTrack.getTrackArtistName(), trackSubtitle);
        } else if (!currentTrack.getTrackArtistName().isEmpty()) {
            trackSubtitle = currentTrack.getTrackArtistName();
        }

        // duration
        String trackDuration = FormatHelper.formatTracktimeFromMS(mContext, currentTrack.getTrackDuration());

        // check if track belongs to an new album
        boolean isNewAlbum = true;
        if (previousTrack != null) {
            isNewAlbum = !previousTrack.getTrackAlbumKey().equals(currentTrack.getTrackAlbumKey());
        }

        CurrentPlaylistItem currentPlaylistItem;
        if (isNewAlbum) {
            if (convertView != null) {
                currentPlaylistItem = (CurrentPlaylistItem) convertView;
                currentPlaylistItem.setTitle(trackTitle);
                currentPlaylistItem.setSubtitle(trackSubtitle);
                currentPlaylistItem.setAddtionalSubtitle(trackDuration);
                currentPlaylistItem.setSectionTitle(currentTrack.getTrackAlbumName());
            } else {
                currentPlaylistItem = new CurrentPlaylistItem(mContext, trackTitle, trackSubtitle, trackDuration, currentTrack.getTrackAlbumName());
            }

            currentPlaylistItem.prepareArtworkFetching(mArtworkManager, currentTrack);

            // Check if the scroll speed currently is already 0, then start the image task right away.
            if (mScrollSpeed == 0) {
                currentPlaylistItem.startCoverImageTask();
            }
        } else {
            if (convertView != null) {
                currentPlaylistItem = (CurrentPlaylistItem) convertView;
                currentPlaylistItem.setTitle(trackTitle);
                currentPlaylistItem.setSubtitle(trackSubtitle);
                currentPlaylistItem.setAddtionalSubtitle(trackDuration);
                currentPlaylistItem.setSectionTitle(null);
            } else {
                currentPlaylistItem = new CurrentPlaylistItem(mContext, trackTitle, trackSubtitle, trackDuration, null);
            }
        }

        if (position == mCurrentPlayingIndex) {
            currentPlaylistItem.setPlaying(true);
        } else {
            currentPlaylistItem.setPlaying(false);
        }

        return currentPlaylistItem;
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
